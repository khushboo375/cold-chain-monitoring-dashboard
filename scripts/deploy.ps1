
$ErrorActionPreference = "Stop"

$workspace = $env:WORKSPACE
if ([string]::IsNullOrWhiteSpace($workspace)) {
    throw "WORKSPACE environment variable is not set."
}

$deployDir = Join-Path $workspace "deploy"
$sourceJar = Join-Path $workspace "target\cold-chain-dashboard.jar"
$targetJar = Join-Path $deployDir "cold-chain-dashboard.jar"
$javaExe = "C:\DevTools\jdk-21.0.12\bin\java.exe"
$port = 8090

if (-not (Test-Path $sourceJar)) {
    throw "Build JAR not found: $sourceJar"
}
if (-not (Test-Path $javaExe)) {
    throw "Java executable not found: $javaExe"
}
if (-not (Test-Path $deployDir)) {
    New-Item -ItemType Directory -Path $deployDir -Force | Out-Null
}

# Stop the previous dashboard Java process only.
$javaProcesses = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "java.exe" -and
        $_.CommandLine -like "*cold-chain-dashboard.jar*--server.port=$port*"
    }

foreach ($process in $javaProcesses) {
    Write-Host "Stopping previous dashboard process PID $($process.ProcessId)..."
    Stop-Process -Id $process.ProcessId -Force
}

# Wait for port 8090 to be released.
$portFree = $false
for ($i = 0; $i -lt 30; $i++) {
    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $listener) {
        $portFree = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $portFree) {
    throw "Port $port is still in use. Deployment stopped safely."
}

# Copy the built artifact into the deployment directory.
Copy-Item -Path $sourceJar -Destination $targetJar -Force
Write-Host "Updated deployment JAR."

# Launch Java directly, detached from the Jenkins PowerShell process.
$stdoutLog = Join-Path $deployDir "app.log"
$stderrLog = Join-Path $deployDir "app-error.log"

$process = Start-Process `
    -FilePath $javaExe `
    -ArgumentList @("-jar", "`"$targetJar`"", "--server.port=$port") `
    -WorkingDirectory $deployDir `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -WindowStyle Hidden `
    -PassThru

Write-Host "Started dashboard process PID $($process.Id)."

# Verify the application becomes healthy.
$healthy = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 3

    $process.Refresh()
    if ($process.HasExited) {
        Get-Content $stderrLog -Tail 40 -ErrorAction SilentlyContinue
        throw "Dashboard process exited with code $($process.ExitCode). Check $stderrLog"
    }

    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$port/health" -TimeoutSec 3
        if ($response.status -eq "UP") {
            $healthy = $true
            break
        }
    }
    catch {
        # The application may still be starting.
    }
}

if (-not $healthy) {
    Get-Content $stderrLog -Tail 40 -ErrorAction SilentlyContinue
    throw "Deployment health check failed on port $port."
}

Write-Host "Deployment successful!"
Write-Host "Health endpoint: http://localhost:$port/health"
