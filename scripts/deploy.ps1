
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

$task = Get-ScheduledTask -TaskName $taskName -ErrorAction Stop

# Stop the previous deployed instance before replacing its JAR.
if ($task.State -eq "Running") {
    Write-Host "Stopping previous dashboard deployment..."
    Stop-ScheduledTask -TaskName $taskName

    $stopped = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        $task = Get-ScheduledTask -TaskName $taskName

        if ($task.State -ne "Running") {
            $stopped = $true
            break
        }
    }

    if (-not $stopped) {
        throw "Previous scheduled task did not stop."
    }
}

# Wait for port 8090 to become free.
for ($i = 0; $i -lt 20; $i++) {
    $listener = Get-NetTCPConnection `
        -LocalPort $port `
        -State Listen `
        -ErrorAction SilentlyContinue

    if (-not $listener) {
        break
    }

    Start-Sleep -Seconds 1
}

$listener = Get-NetTCPConnection `
    -LocalPort $port `
    -State Listen `
    -ErrorAction SilentlyContinue

if ($listener) {
    throw "Port $port is still in use. Deployment stopped safely."
}

# Copy the newly built artifact after stopping the old instance.
Copy-Item -Path $sourceJar -Destination $targetJar -Force
Write-Host "Updated deployment JAR."

# Launch the application independently through Windows Task Scheduler.
Write-Host "Starting scheduled task: $taskName"
Start-ScheduledTask -TaskName $taskName

# Wait up to 90 seconds for the health endpoint.
$healthy = $false

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 3

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
    throw "Deployment health check failed on port $port. Check the scheduled task and application logs."
}
