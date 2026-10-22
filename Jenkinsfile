
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
        TEST_PORT = '8091'
        DEPLOY_PORT = '8092'
        APP_NAME = 'cold-chain-dashboard'
        DOCKER_IMAGE = 'khushboo375/cold-chain-dashboard'
        CONTAINER_NAME = 'cold-chain-container'
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
                    $java = Join-Path $env:JAVA_HOME "bin\\java.exe"

                    if (-not (Test-Path $jar)) {
                        throw "Application JAR not found: $jar"
                    }

                    if (-not (Test-Path $java)) {
                        throw "Java executable not found: $java"
                    }

                    $listener = Get-NetTCPConnection `
                        -LocalPort 8091 -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($listener) {
                        throw "Port 8091 is already in use."
                    }

                    Remove-Item $outLog, $errLog `
                        -Force -ErrorAction SilentlyContinue

                    $command = '"' + $java + '" -jar "' + $jar +
                        '" --server.port=8091 1>"' + $outLog +
                        '" 2>"' + $errLog + '"'

                    Start-Process `
                        -FilePath $env:ComSpec `
                        -ArgumentList @('/d', '/s', '/c', "start `"`" /b $command") `
                        -WorkingDirectory $env:WORKSPACE

                    $healthy = $false

                    for ($i = 0; $i -lt 40; $i++) {
                        Start-Sleep -Seconds 3

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
                            # Keep waiting for Spring Boot.
                        }
                    }

                    if (-not $healthy) {
                        Get-Content $outLog -ErrorAction SilentlyContinue
                        Get-Content $errLog -ErrorAction SilentlyContinue
                        throw "Test application did not become healthy."
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

        stage('Test Docker from Jenkins') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    Write-Host "===== Windows account ====="
                    whoami

                    Write-Host "===== Docker version ====="
                    docker version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot connect to Docker."
                    }

                    Write-Host "===== Docker contexts ====="
                    docker context ls

                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot list Docker contexts."
                    }

                    Write-Host "===== Buildx builders ====="
                    docker buildx ls

                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot list Docker Buildx builders."
                    }

                    Write-Host "===== Test image build ====="
                    docker build -t coldchain-jenkins-test .

                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins could not build a test Docker image."
                    }

                    Write-Host "===== Test image created ====="
                    docker image inspect coldchain-jenkins-test

                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot inspect the test image."
                    }

                    Write-Host "Docker test passed."
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    docker build `
                        -t "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER" `
                        -t "$env:DOCKER_IMAGE`:latest" .

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image build failed."
                    }

                    docker image inspect "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"

                    if ($LASTEXITCODE -ne 0) {
                        throw "Versioned Docker image was not created."
                    }
                '''
            }
        }

        
        stage('Push Image to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-khushboo375-v2',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        Write-Host "Authenticating to Docker Hub..."

                        $env:DOCKERHUB_TOKEN |
                            docker login `
                                --username $env:DOCKERHUB_USER `
                                --password-stdin

                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker Hub authentication failed. Check the Jenkins credential."
                        }

                        Write-Host "Docker Hub authentication succeeded."

                        $versionedImage = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                        $latestImage = "$env:DOCKER_IMAGE`:latest"

                        docker push $versionedImage

                        if ($LASTEXITCODE -ne 0) {
                            throw "Push failed for $versionedImage."
                        }

                        docker push $latestImage

                        if ($LASTEXITCODE -ne 0) {
                            throw "Push failed for $latestImage."
                        }

                        Write-Host "Both image tags pushed successfully."
                    '''
                }
            }
        }


        stage('Deploy Docker Container') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    $image = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"

                    docker pull $image

                    if ($LASTEXITCODE -ne 0) {
                        throw "Could not pull image $image."
                    }

                    docker rm -f $env:CONTAINER_NAME 2>$null
                    $global:LASTEXITCODE = 0

                    docker run -d `
                        --name $env:CONTAINER_NAME `
                        --restart unless-stopped `
                        -p 8092:8080 `
                        $image

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker container failed to start."
                    }

                    $healthy = $false

                    for ($i = 0; $i -lt 40; $i++) {
                        Start-Sleep -Seconds 3

                        try {
                            $response = Invoke-RestMethod `
                                -Uri "http://localhost:8092/health" `
                                -TimeoutSec 3

                            if ($response.status -eq "UP") {
                                $healthy = $true
                                break
                            }
                        }
                        catch {
                            # Keep waiting for the container.
                        }
                    }

                    if (-not $healthy) {
                        docker logs $env:CONTAINER_NAME
                        throw "Deployment health check failed on port 8092."
                    }

                    Write-Host "Deployment successful!"
                    Write-Host "Image: $image"
                    Write-Host "Application: http://localhost:8092"

                    docker ps --filter "name=$env:CONTAINER_NAME"
                '''
            }
        }
    }

    post {
        always {
            // Preserve test results and evidence.
            junit(
                testResults: 'target/surefire-reports/TEST-*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,target/selenium-screenshots/**,target/selenium-app-*.log',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        success {
            echo 'Build, Selenium tests, Docker image publishing, and container deployment succeeded.'
        }

        failure {
            echo 'Pipeline failed. Check the console output and published test reports.'
        }
    }
}
