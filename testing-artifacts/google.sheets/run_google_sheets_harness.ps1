# Drives the google.sheets dispatch-verification harness. Unlike Calendar/Drive, no mock server
# is needed - the trigger dispatches directly from the inbound POST body with zero outbound calls
# of its own, so the driver just POSTs synthetic payloads straight at the harness listener.
#
# Usage: powershell -File run_google_sheets_harness.ps1

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$balExe = "C:\Program Files\Ballerina\bin\bal.bat"

$harnessDir = Join-Path $root "harness"
$payloadsDir = Join-Path $root "payloads"

$scenarios = @(
    @{ Name = "appendRow"; Action = "onAppendRow"; File = "event_append_row.json"; ExpectedLine = "FIRED::SheetRowService::onAppendRow" },
    @{ Name = "updateRow"; Action = "onUpdateRow"; File = "event_update_row.json"; ExpectedLine = "FIRED::SheetRowService::onUpdateRow" }
)

Write-Host "Building harness..."
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
Start-Sleep -Seconds 2

$results = @()

try {
    foreach ($scenario in $scenarios) {
        Write-Host "--- Testing $($scenario.Name) ---"

        $fixturePath = Join-Path $payloadsDir $scenario.File
        $body = Get-Content $fixturePath -Raw

        $response = Invoke-WebRequest -Method Post -Uri "http://localhost:8090/" `
            -ContentType "application/json" -Body $body -UseBasicParsing

        Start-Sleep -Milliseconds 500

        $log = Get-Content $harnessStdout -Raw -ErrorAction SilentlyContinue
        $fired = $log -and $log.Contains($scenario.ExpectedLine)

        $results += [PSCustomObject]@{
            Action     = $scenario.Action
            HttpStatus = $response.StatusCode
            Fired      = $fired
        }

        if ($fired) {
            Write-Host "  PASS - $($scenario.ExpectedLine) found"
        } else {
            Write-Host "  FAIL - $($scenario.ExpectedLine) NOT found"
        }
    }
}
finally {
    Write-Host "Stopping harness..."
    Stop-Process -Id $harnessProc.Id -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Results ==="
$results | Format-Table -AutoSize
$passCount = ($results | Where-Object { $_.Fired }).Count
Write-Host "$passCount / $($results.Count) passed"

$results | Export-Csv -Path (Join-Path $root "test_results.csv") -NoTypeInformation
