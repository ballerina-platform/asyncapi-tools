# Drives the google.mail dispatch-verification harness against the local mock Pub/Sub + Gmail
# API. See ../../.claude/plans (or the session writeup) for the full design rationale.
#
# Usage: powershell -File run_google_mail_harness.ps1

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$balExe = "C:\Program Files\Ballerina\bin\bal.bat"

$mockDir = Join-Path $root "mock"
$harnessDir = Join-Path $root "harness"

# The mock's subscription-create response always echoes this fixed name (see mock_service.bal's
# FIXED_SUBSCRIPTION_RESOURCE), regardless of the uuid-generated name the trigger actually
# requests - that uuid isn't knowable in advance, so the mock decouples it from what the driver
# needs to send as the simulated Pub/Sub push's "subscription" field.
$fixedSubscriptionResource = "projects/mock-project/subscriptions/test-subscription-fixed"

$scenarios = @(
    @{ Name = "scenario1"; Actions = @("onNewEmail");                        ExpectedLines = @("FIRED::GmailService::onNewEmail") },
    @{ Name = "scenario2"; Actions = @("onNewEmail", "onNewThread");         ExpectedLines = @("FIRED::GmailService::onNewEmail", "FIRED::GmailService::onNewThread") },
    @{ Name = "scenario3"; Actions = @("onNewEmail", "onNewAttachment");    ExpectedLines = @("FIRED::GmailService::onNewEmail", "FIRED::GmailService::onNewAttachment") },
    @{ Name = "scenario4"; Actions = @("onEmailLabelAdded");                 ExpectedLines = @("FIRED::GmailService::onEmailLabelAdded") },
    @{ Name = "scenario5"; Actions = @("onEmailLabelAdded", "onEmailStarred"); ExpectedLines = @("FIRED::GmailService::onEmailLabelAdded", "FIRED::GmailService::onEmailStarred") },
    @{ Name = "scenario6"; Actions = @("onEmailLabelRemoved");               ExpectedLines = @("FIRED::GmailService::onEmailLabelRemoved") },
    @{ Name = "scenario7"; Actions = @("onEmailLabelRemoved", "onEmailStarRemoved"); ExpectedLines = @("FIRED::GmailService::onEmailLabelRemoved", "FIRED::GmailService::onEmailStarRemoved") }
)

Write-Host "Building mock and harness..."
Push-Location $mockDir
& $balExe build > (Join-Path $mockDir "mock_build.log")
Pop-Location
Push-Location $harnessDir
& $balExe build > (Join-Path $harnessDir "harness_build.log")
Pop-Location

function Wait-ForPort {
    param([int]$Port, [int]$TimeoutSeconds = 60)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $client.Connect("localhost", $Port)
            $client.Close()
            return $true
        } catch {
            Start-Sleep -Milliseconds 1000
        }
    }
    return $false
}

Write-Host "Starting mock server (port 8091)..."
$mockProc = Start-Process -FilePath $balExe -ArgumentList "run" -WorkingDirectory $mockDir `
    -RedirectStandardOutput (Join-Path $mockDir "mock_stdout.log") `
    -RedirectStandardError (Join-Path $mockDir "mock_stderr.log") `
    -PassThru -WindowStyle Hidden
if (-not (Wait-ForPort -Port 8091 -TimeoutSeconds 60)) {
    throw "Mock server did not come up on port 8091 within 60s"
}
Write-Host "  mock is up"

Write-Host "Starting harness listener (port 8090)..."
$harnessStdout = Join-Path $harnessDir "harness_stdout.log"
$harnessProc = Start-Process -FilePath $balExe -ArgumentList "run" -WorkingDirectory $harnessDir `
    -RedirectStandardOutput $harnessStdout `
    -RedirectStandardError (Join-Path $harnessDir "harness_stderr.log") `
    -PassThru -WindowStyle Hidden
if (-not (Wait-ForPort -Port 8090 -TimeoutSeconds 60)) {
    throw "Harness did not come up on port 8090 within 60s"
}
Write-Host "  harness is up"
# Extra buffer: port accepting connections doesn't guarantee attach() (topic/subscription
# provisioning + watchMailbox(), all of which call out to the mock) has finished yet.
Start-Sleep -Seconds 5

$results = @()

try {
    foreach ($scenario in $scenarios) {
        Write-Host "--- Testing $($scenario.Name) ---"

        # Same action (e.g. onEmailLabelAdded) can be the expected line for more than one
        # scenario, so only inspect log content written *after* this scenario's POST - otherwise
        # a line left over from an earlier scenario would produce a false PASS here.
        $beforeLength = 0
        if (Test-Path $harnessStdout) {
            $beforeLength = (Get-Item $harnessStdout).Length
        }

        # Tell the mock which history/message/thread fixtures the next calls should return.
        Invoke-RestMethod -Method Post -Uri "http://localhost:8091/control/scenario" `
            -ContentType "application/json" -Body (@{ scenario = $scenario.Name } | ConvertTo-Json) | Out-Null

        # Simulate the Pub/Sub push Gmail would send after watchMailbox() registration.
        $response = Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" `
            -ContentType "application/json" `
            -Body (@{ subscription = $fixedSubscriptionResource } | ConvertTo-Json) -UseBasicParsing

        Start-Sleep -Milliseconds 800

        $fullLog = Get-Content $harnessStdout -Raw -ErrorAction SilentlyContinue
        $log = ""
        if ($fullLog -and $fullLog.Length -ge $beforeLength) {
            $log = $fullLog.Substring($beforeLength)
        }

        foreach ($i in 0..($scenario.Actions.Count - 1)) {
            $action = $scenario.Actions[$i]
            $expectedLine = $scenario.ExpectedLines[$i]
            $fired = $log -and $log.Contains($expectedLine)

            $results += [PSCustomObject]@{
                Scenario   = $scenario.Name
                Action     = $action
                HttpStatus = $response.StatusCode
                Fired      = $fired
            }

            if ($fired) {
                Write-Host "  PASS - $expectedLine found"
            } else {
                Write-Host "  FAIL - $expectedLine NOT found"
            }
        }
    }
}
finally {
    Write-Host "Stopping harness and mock..."
    Stop-Process -Id $harnessProc.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $mockProc.Id -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Results ==="
$results | Format-Table -AutoSize
$passCount = ($results | Where-Object { $_.Fired }).Count
Write-Host "$passCount / $($results.Count) passed"

$results | Export-Csv -Path (Join-Path $root "test_results.csv") -NoTypeInformation
