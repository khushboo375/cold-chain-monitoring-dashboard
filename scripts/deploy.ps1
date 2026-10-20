
$ErrorActionPreference = "Stop"

$workspace = $env:WORKSPACE
$deployDir = Join-Path $workspace "deploy"
$sourceJar = Join-Path $workspace "target\cold-chain-dashboard.jar"
$targetJar = Join-Path $deployDir "cold-chain-dashboard.jar"
$taskName = "ColdChainDashboard"
$port = 8090

if (-not (Test-Path $sourceJar)) {
    throw "Build JAR not found: $sourceJar"
}

if (-not (Test-Path $deployDir)) {
    New-Item -ItemType Directory -Path $deployDir -Force | Out-Null
}

# Stop the previous scheduled task instance.
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction Stop

if ($task.State -eq "Running") {
    Stop-ScheduledTask -TaskName $taskName

    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        $task = Get-ScheduledTask -TaskName $taskName

        if ($task.State -ne "Running") {
            break
        }
    }
}

# Stop only a Java process running this dashboard JAR.
$javaProcesses = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "java.exe" -and
        $_.CommandLine -like "*-jar cold-chain-dashboard.jar*"
    }

foreach ($process in $javaProcesses) {
    Write-Host "Stopping previous dashboard process PID $($process.ProcessId)..."
    Stop-Process -Id $process.ProcessId -Force
}

# Wait for port 8090 to be released.
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
    throw "Port $port is still in use. Deployment stopped safely."
}

# Copy the new build artifact.
Copy-Item -Path $sourceJar -Destination $targetJar -Force
Write-Host "Updated deployment JAR."

# Start the application using the working scheduled task.
Write-Host "Starting scheduled task: $taskName"
Start-ScheduledTask -TaskName $taskName

# Wait for the health endpoint.
$healthy = $false

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 3

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
    throw "Deployment health check failed on port $port. Check Task Scheduler history and application logs."
}

Write-Host "Deployment successful!"
Write-Host "Health endpoint: http://localhost:$port/health"
