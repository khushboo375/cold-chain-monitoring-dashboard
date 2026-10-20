
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

# Stop the scheduled task if it is running.
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction Stop

if ($task.State -eq "Running") {
    Stop-ScheduledTask -TaskName $taskName
}

# Find and stop only the Java process running our dashboard JAR.
$javaProcesses = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "java.exe" -and
        $_.CommandLine -like "*-jar cold-chain-dashboard.jar*"
    }

foreach ($process in $javaProcesses) {
    Write-Host "Stopping previous dashboard process PID $($process.ProcessId)..."
    Stop-Process -Id $process.ProcessId -Force
}

# Wait for port 8090 to become free.
$portFree = $false

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1

    $listener = Get-NetTCPConnection `
        -LocalPort $port `
        -State Listen `
        -ErrorAction SilentlyContinue

    if (-not $listener) {
        $portFree = $true
        break
    }
}

if (-not $portFree) {
    throw "Port $port is still in use. Check the process listening on that port."
}

# Deploy the newly built JAR.
Copy-Item -Path $sourceJar -Destination $targetJar -Force
Write-Host "Updated deployment JAR."

# Start the application independently of the Jenkins process.
Write-Host "Starting scheduled task: $taskName"
Start-ScheduledTask -TaskName $taskName

# Verify application health for up to 90 seconds.
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
        # Wait while Spring Boot starts.
    }
}

if (-not $healthy) {
    throw "Deployment health check failed on port $port."
}
