
$ErrorActionPreference = "Stop"

$workspace = $env:WORKSPACE
$deployDir = Join-Path $workspace "deploy"
$sourceJar = Join-Path $workspace "target\cold-chain-dashboard.jar"
$targetJar = Join-Path $deployDir "cold-chain-dashboard.jar"
$javaExe = "C:\DevTools\jdk-21.0.12\bin\java.exe"
$port = 8090

$stdoutLog = Join-Path $deployDir "app.log"
$stderrLog = Join-Path $deployDir "app-error.log"
$pidFile = Join-Path $deployDir "app.pid"

if (-not (Test-Path $sourceJar)) {
    throw "Build JAR not found: $sourceJar"
}

if (-not (Test-Path $javaExe)) {
    throw "Java executable not found: $javaExe"
}

New-Item -ItemType Directory -Path $deployDir -Force | Out-Null

# Stop the previous deployment, if its PID file identifies a process
# running this exact application JAR.
if (Test-Path $pidFile) {
    $oldPidText = (Get-Content $pidFile -Raw).Trim()
    $oldPid = 0

    if ([int]::TryParse($oldPidText, [ref]$oldPid)) {
        $oldProcess = Get-CimInstance Win32_Process `
            -Filter "ProcessId = $oldPid" `
            -ErrorAction SilentlyContinue

        if (
            $oldProcess -and
            $oldProcess.Name -eq "java.exe" -and
            $oldProcess.CommandLine -like "*-jar cold-chain-dashboard.jar*"
        ) {
            Write-Host "Stopping previous deployment PID $oldPid..."
            Stop-Process -Id $oldPid -Force
        }
    }

    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

# Wait for the old application to release the port.
$portFree = $false

for ($i = 0; $i -lt 30; $i++) {
    $listener = Get-NetTCPConnection `
        -LocalPort $port `
        -State Listen `
        -ErrorAction SilentlyContinue

    if (-not $listener) {
        $portFree = $true
        break
    }

    Start-Sleep -Seconds 1
}

if (-not $portFree) {
    throw "Port $port is still in use. The existing listener was not stopped."
}

# Install the new artifact.
Copy-Item -Path $sourceJar -Destination $targetJar -Force
Write-Host "Updated deployment JAR."

# Clear old logs so this deployment's startup output is easy to inspect.
Set-Content -Path $stdoutLog -Value ""
Set-Content -Path $stderrLog -Value ""

# Launch Java independently of the Jenkins build process.
$javaProcess = Start-Process `
    -FilePath $javaExe `
    -ArgumentList @("-jar", "`"$targetJar`"", "--server.port=$port") `
    -WorkingDirectory $deployDir `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Set-Content -Path $pidFile -Value $javaProcess.Id
Write-Host "Started Java process PID $($javaProcess.Id)."

# Verify health for up to 90 seconds.
$healthy = $false

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 3

    $javaProcess.Refresh()

    if ($javaProcess.HasExited) {
        Get-Content $stderrLog -ErrorAction SilentlyContinue
        Get-Content $stdoutLog -ErrorAction SilentlyContinue
        throw "Java exited with code $($javaProcess.ExitCode). Check app-error.log and app.log."
    }

    try {
        $response = Invoke-RestMethod `
            -Uri "http://localhost:$port/health" `
            -TimeoutSec 3

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
    Write-Host "Application startup logs:"
    Get-Content $stdoutLog -ErrorAction SilentlyContinue -Tail 60
    Get-Content $stderrLog -ErrorAction SilentlyContinue -Tail 60
    throw "Deployment health check failed on port $port."
}

Write-Host "Deployment successful!"
Write-Host "Health endpoint: http://localhost:$port/health"
Write-Host "Application log: $stdoutLog"
