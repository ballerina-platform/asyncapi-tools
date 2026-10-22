# Drives the google.calendar dispatch-verification harness against the local mock Google API.
# See ../../.claude/plans (or the session writeup) for the full design rationale.
#
# Usage: powershell -File run_google_calendar_harness.ps1

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$balExe = "C:\Program Files\Ballerina\bin\bal.bat"

$mockDir = Join-Path $root "mock"
$harnessDir = Join-Path $root "harness"

$scenarios = @(
    @{ Name = "new";    Action = "onNewEvent";    ExpectedLine = "FIRED::CalendarService::onNewEvent" },
    @{ Name = "update"; Action = "onEventUpdate"; ExpectedLine = "FIRED::CalendarService::onEventUpdate" },
    @{ Name = "delete"; Action = "onEventDelete"; ExpectedLine = "FIRED::CalendarService::onEventDelete" }
)

Write-Host "Building mock and harness..."
Push-Location $mockDir
& $balExe build *> (Join-Path $mockDir "mock_build.log")
Pop-Location
Push-Location $harnessDir
& $balExe build *> (Join-Path $harnessDir "harness_build.log")
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
# Extra buffer: port accepting connections doesn't guarantee attach()/registerWatchChannel()
# (which itself calls out to the mock) has finished yet.
Start-Sleep -Seconds 5

$results = @()

try {
    foreach ($scenario in $scenarios) {
        Write-Host "--- Testing $($scenario.Name) ---"

        # Tell the mock which event fixture the next GET .../events call should return.
        Invoke-RestMethod -Method Post -Uri "http://localhost:8091/control/scenario" `
            -ContentType "application/json" -Body (@{ scenario = $scenario.Name } | ConvertTo-Json) | Out-Null

        # Simulate the "something changed" ping Google would send, with the known
        # channel/resource IDs the mock's /events/watch response established.
        $response = Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" `
            -Headers @{
                "X-Goog-Channel-ID"     = "test-channel-id"
                "X-Goog-Resource-ID"    = "test-resource-id"
                "X-Goog-Resource-State" = "exists"
            } `
            -ContentType "application/json" -Body "{}" -UseBasicParsing

        Start-Sleep -Milliseconds 500

        $log = Get-Content $harnessStdout -Raw -ErrorAction SilentlyContinue
        $fired = $log -and $log.Contains($scenario.ExpectedLine)

        $results += [PSCustomObject]@{
            Action       = $scenario.Action
            HttpStatus   = $response.StatusCode
            Fired        = $fired
        }

        if ($fired) {
            Write-Host "  PASS - $($scenario.ExpectedLine) found"
        } else {
            Write-Host "  FAIL - $($scenario.ExpectedLine) NOT found"
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
