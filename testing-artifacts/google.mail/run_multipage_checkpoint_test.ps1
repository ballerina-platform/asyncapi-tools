# Verifies the CodeRabbit-flagged multi-page fix directly: page 1 fully succeeds and would offer
# its own historyId as a checkpoint, but page 2 (reached via nextPageToken) then fails. The
# checkpoint after that must land on page 1's successfully-dispatched item - not on page 1's own
# historyId (the bug: that gets persisted before we even know page 2 will succeed), and not on
# page 2's historyId either (page 2 never actually completed).
#
# Usage: powershell -File run_multipage_checkpoint_test.ps1

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$balExe = "C:\Program Files\Ballerina\bin\bal.bat"

$mockDir = Join-Path $root "mock"
$harnessDir = Join-Path $root "harness"
$fixedSubscriptionResource = "projects/mock-project/subscriptions/test-subscription-fixed"

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
Start-Sleep -Seconds 5

$allPassed = $true

try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8091/control/scenario" `
        -ContentType "application/json" -Body (@{ scenario = "scenario9" } | ConvertTo-Json) | Out-Null

    # --- Push 1: page 1 (succeeds) -> page 2 (fails) ---
    Write-Host "--- Push 1: page 1 succeeds, page 2 (via nextPageToken) fails ---"
    $beforeLength = 0
    if (Test-Path $harnessStdout) { $beforeLength = (Get-Item $harnessStdout).Length }

    Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" -ContentType "application/json" `
        -Body (@{ subscription = $fixedSubscriptionResource } | ConvertTo-Json) -UseBasicParsing | Out-Null
    Start-Sleep -Milliseconds 800

    $fullLog = Get-Content $harnessStdout -Raw -ErrorAction SilentlyContinue
    $log = ""
    if ($fullLog -and $fullLog.Length -ge $beforeLength) { $log = $fullLog.Substring($beforeLength) }

    $successFired = $log -and $log.Contains("FIRED::GmailService::onNewEmail")
    if ($successFired) {
        Write-Host "  PASS - page 1's item dispatched (FIRED::GmailService::onNewEmail found)"
    } else {
        Write-Host "  FAIL - page 1's item never dispatched"
        $allPassed = $false
    }

    $push1LastStartId = (Invoke-RestMethod -Method Get -Uri "http://localhost:8091/control/lastStartId").lastStartHistoryId
    Write-Host "  Push 1's calls used startHistoryId = '$push1LastStartId' (the pre-batch value, expected)"

    # --- Push 2: same scenario again, checking what checkpoint the dispatcher now uses ---
    Write-Host "--- Push 2: checking what checkpoint the dispatcher now uses ---"
    Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" -ContentType "application/json" `
        -Body (@{ subscription = $fixedSubscriptionResource } | ConvertTo-Json) -UseBasicParsing | Out-Null
    Start-Sleep -Milliseconds 800

    $push2LastStartId = (Invoke-RestMethod -Method Get -Uri "http://localhost:8091/control/lastStartId").lastStartHistoryId
    Write-Host "  Push 2's history call used startHistoryId = '$push2LastStartId'"

    Write-Host ""
    Write-Host "=== Checkpoint assertions ==="

    if ($push2LastStartId -eq "9001") {
        Write-Host "  PASS - checkpoint advanced to page 1's successful entry id ('9001')"
    } else {
        Write-Host "  FAIL - expected checkpoint '9001', got '$push2LastStartId'"
        $allPassed = $false
    }

    if ($push2LastStartId -ne $push1LastStartId) {
        Write-Host "  PASS - checkpoint moved forward between push 1 and push 2"
    } else {
        Write-Host "  FAIL - checkpoint did not move at all"
        $allPassed = $false
    }

    if ($push2LastStartId -ne "9100") {
        Write-Host "  PASS - checkpoint did NOT prematurely save page 1's own historyId ('9100') - the CodeRabbit-flagged bug"
    } else {
        Write-Host "  FAIL - checkpoint saved page 1's historyId before page 2 was confirmed - the exact bug CodeRabbit flagged"
        $allPassed = $false
    }

    if ($push2LastStartId -ne "9999") {
        Write-Host "  PASS - checkpoint did NOT jump to page 2's historyId ('9999') either - page 2 never completed"
    } else {
        Write-Host "  FAIL - checkpoint jumped to page 2's historyId despite page 2 failing"
        $allPassed = $false
    }
}
finally {
    Write-Host ""
    Write-Host "Stopping harness and mock..."
    # Stop-Process only kills the bal.bat wrapper PID, not the java.exe it spawns underneath -
    # taskkill /T kills the whole process tree instead, so nothing is left running as an orphan.
    taskkill /PID $harnessProc.Id /T /F 2>$null | Out-Null
    taskkill /PID $mockProc.Id /T /F 2>$null | Out-Null
}

Write-Host ""
if ($allPassed) {
    Write-Host "ALL CHECKS PASSED"
} else {
    Write-Host "SOME CHECKS FAILED"
    exit 1
}
