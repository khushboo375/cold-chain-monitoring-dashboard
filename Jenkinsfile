
pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_PORT = '8090'
        TEST_PORT = '8091'
        APP_NAME = 'cold-chain-dashboard'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Application') {
            steps {
                bat 'mvn -B clean package -DskipTests'

                archiveArtifacts(
                    artifacts: 'target/cold-chain-dashboard.jar',
                    fingerprint: true
                )
            }
        }

        stage('Start Test Application') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    $jar = Join-Path $env:WORKSPACE "target\\cold-chain-dashboard.jar"
                    $outLog = Join-Path $env:WORKSPACE "target\\selenium-app-out.log"
                    $errLog = Join-Path $env:WORKSPACE "target\\selenium-app-err.log"
                    $pidFile = Join-Path $env:WORKSPACE "target\\selenium-app.pid"

                    if (-not (Test-Path $jar)) {
                        throw "Application JAR not found: $jar"
                    }

                    $javaHome = $env:JAVA_HOME
                    if (-not $javaHome) {
                        throw "JAVA_HOME is not configured by Jenkins."
                    }

                    $java = Join-Path $javaHome "bin\\java.exe"
                    if (-not (Test-Path $java)) {
                        throw "Java executable not found: $java"
                    }

                    $existing = Get-NetTCPConnection `
                        -LocalPort 8091 -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {
                        throw "Port 8091 is already in use. Stop the existing process before testing."
                    }

                    $arguments = '-jar "{0}" --server.port=8091' -f $jar

                    $process = Start-Process `
                        -FilePath $java `
                        -ArgumentList $arguments `
                        -WorkingDirectory $env:WORKSPACE `
                        -RedirectStandardOutput $outLog `
                        -RedirectStandardError $errLog `
                        -PassThru

                    Set-Content -Path $pidFile -Value $process.Id

                    $healthy = $false

                    for ($i = 0; $i -lt 40; $i++) {
                        Start-Sleep -Seconds 3

                        $process.Refresh()

                        if ($process.HasExited) {
                            Get-Content $outLog -ErrorAction SilentlyContinue
                            Get-Content $errLog -ErrorAction SilentlyContinue
                            throw "Test application exited during startup. Check the Selenium application logs."
                        }

                        try {
                            $response = Invoke-RestMethod `
                                -Uri "http://localhost:8091/health" `
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
                        Get-Content $outLog -ErrorAction SilentlyContinue
                        Get-Content $errLog -ErrorAction SilentlyContinue
                        throw "Test application did not become healthy on port 8091."
                    }

                    Write-Host "Test application is healthy on port 8091."
                '''
            }
        }

        stage('Selenium Continuous Tests') {
            steps {
                bat 'mvn -B test "-Dselenium.baseUrl=http://localhost:8091"'
            }
        }

        stage('Deploy Application') {
            steps {
                bat 'powershell -NoProfile -ExecutionPolicy Bypass -File scripts\\deploy.ps1'
            }
        }
    }

    post {
        always {
            // Stop the temporary test application first.
            powershell returnStatus: true, script: '''
                $pidFile = Join-Path $env:WORKSPACE "target\\selenium-app.pid"

                if (Test-Path $pidFile) {
                    $appPid = (Get-Content $pidFile -Raw).Trim()

                    if ($appPid -match '^\\d+$') {
                        $process = Get-Process `
                            -Id ([int]$appPid) `
                            -ErrorAction SilentlyContinue

                        if ($process) {
                            Stop-Process -Id ([int]$appPid) -Force
                            Write-Host "Stopped temporary Selenium test application."
                        }
                    }

                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                }
            '''

            // Publish test results even if the Selenium stage fails.
            junit(
                testResults: 'target/surefire-reports/TEST-*.xml',
                allowEmptyResults: true
            )

            // Preserve reports, screenshots and application logs as evidence.
            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,target/selenium-screenshots/**,target/selenium-app-*.log',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        success {
            echo 'Build, Selenium tests, report publishing, and deployment succeeded.'
        }

        failure {
            echo 'Pipeline failed. Check the console output and published test reports.'
        }
    }
}
