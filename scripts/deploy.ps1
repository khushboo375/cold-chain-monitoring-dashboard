
$ErrorActionPreference = "Stop"

$deployDir = Join-Path $env:WORKSPACE "deploy"
$jarPath = Join-Path $deployDir "cold-chain-dashboard.jar"
$pidFile = Join-Path $deployDir "app.pid"
$stdoutLog = Join-Path $deployDir "app.log"
$stderrLog = Join-Path $deployDir "app-error.log"
$port = 8090

if (-not (Test-Path $jarPath)) {
    throw "Deployment JAR not found: $jarPath"
}

# Stop only the instance previously started by this script.
if (Test-Path $pidFile) {
    $oldPidText = Get-Content $pidFile -Raw
    $oldPid = 0

    if ([int]::TryParse($oldPidText.Trim(), [ref]$oldPid)) {
        $oldProcess = Get-Process -Id $oldPid -ErrorAction SilentlyContinue

        if ($oldProcess -and $oldProcess.ProcessName -eq "java") {
            Stop-Process -Id $oldPid -Force
            Start-Sleep -Seconds 2
        }
    }

    Remove-Item $pidFile -Force
}

$javaPath = Join-Path $env:JAVA_HOME "bin\java.exe"

if (-not (Test-Path $javaPath)) {
    throw "Java executable not found: $javaPath"
}

Write-Host "Starting Cold Chain Dashboard on port $port..."

$arguments = "-jar `"$jarPath`" --server.port=$port"

$app = Start-Process `
    -FilePath $javaPath `
    -ArgumentList $arguments `
    -WorkingDirectory $deployDir `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Set-Content -Path $pidFile -Value $app.Id

# Wait up to 90 seconds for the health endpoint.
$healthy = $false

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 3

    $app.Refresh()

    if ($app.HasExited) {
        Write-Host "Application process exited unexpectedly."
        break
    }

    try {
        $response = Invoke-RestMethod `
            -Uri "http://localhost:$port/health" `
            -TimeoutSec 3

        if ($response.status -eq "UP") {
            Write-Host "Deployment successful!"
            Write-Host "Health endpoint: http://localhost:$port/health"
            $healthy = $true
            break
        }
    }
    catch {
        # The application may still be starting.
    }
}

if (-not $healthy) {
    Write-Host "Application logs:"
    if (Test-Path $stdoutLog) {
        Get-Content $stdoutLog -Tail 40
    }

    if (Test-Path $stderrLog) {
        Get-Content $stderrLog -Tail 40
    }

    throw "Deployment health check failed on port $port."
}
