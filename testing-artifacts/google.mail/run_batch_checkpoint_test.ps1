# Verifies the batch-checkpoint fix (issue #9020) directly: a history batch with two items,
# where the first succeeds and the second (intentionally) fails, must leave the persisted
# checkpoint at the successful item - not at the pre-batch value (the original bug: mid-batch
# failure discards already-processed progress) and not past the failed item either (would silently
# skip retrying it). Verified by inspecting what startHistoryId each push's history call actually
# used, via the mock's /control/lastStartId endpoint - the only externally-observable signal,
# since the mock's fixtures are static and don't vary by the incoming startHistoryId.
#
# Usage: powershell -File run_batch_checkpoint_test.ps1

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
        -ContentType "application/json" -Body (@{ scenario = "scenario8" } | ConvertTo-Json) | Out-Null

    # --- Push 1: batch of [success item, poison item] ---
    Write-Host "--- Push 1: batch with a success item followed by a failing item ---"
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
        Write-Host "  PASS - success item dispatched (FIRED::GmailService::onNewEmail found)"
    } else {
        Write-Host "  FAIL - success item never dispatched"
        $allPassed = $false
    }

    $push1LastStartId = (Invoke-RestMethod -Method Get -Uri "http://localhost:8091/control/lastStartId").lastStartHistoryId
    Write-Host "  Push 1's own history call used startHistoryId = '$push1LastStartId' (the pre-batch value, expected)"

    # --- Push 2: same batch again (simulating the trigger being pinged again) ---
    Write-Host "--- Push 2: same batch, checking what checkpoint the dispatcher now uses ---"
    Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" -ContentType "application/json" `
        -Body (@{ subscription = $fixedSubscriptionResource } | ConvertTo-Json) -UseBasicParsing | Out-Null
    Start-Sleep -Milliseconds 800

    $push2LastStartId = (Invoke-RestMethod -Method Get -Uri "http://localhost:8091/control/lastStartId").lastStartHistoryId
    Write-Host "  Push 2's history call used startHistoryId = '$push2LastStartId'"

    Write-Host ""
    Write-Host "=== Checkpoint assertions ==="

    # The checkpoint tracks the outer History entry's own "id" (here "8001"), not the id of the
    # message nested inside its messagesAdded[] - that's true of the original code too, this fix
    # doesn't change which field is used, only when it gets persisted.
    if ($push2LastStartId -eq "8001") {
        Write-Host "  PASS - checkpoint advanced to the successful entry's own id ('8001')"
    } else {
        Write-Host "  FAIL - expected checkpoint '8001', got '$push2LastStartId'"
        $allPassed = $false
    }

    if ($push2LastStartId -ne $push1LastStartId) {
        Write-Host "  PASS - checkpoint moved forward between push 1 and push 2 (not stuck at the pre-batch value)"
    } else {
        Write-Host "  FAIL - checkpoint did not move at all - this is the original bug (#9020)"
        $allPassed = $false
    }

    if ($push2LastStartId -ne "8999") {
        Write-Host "  PASS - checkpoint did NOT jump past the still-failing item to the page-level id ('8999')"
    } else {
        Write-Host "  FAIL - checkpoint jumped past the failed item - it would never be retried"
        $allPassed = $false
    }
}
finally {
    Write-Host ""
    Write-Host "Stopping harness and mock..."
    Stop-Process -Id $harnessProc.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $mockProc.Id -Force -ErrorAction SilentlyContinue
}

Write-Host ""
if ($allPassed) {
    Write-Host "ALL CHECKS PASSED"
} else {
    Write-Host "SOME CHECKS FAILED"
    exit 1
}
