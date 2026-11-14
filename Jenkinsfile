// Jenkins Declarative Pipeline equivalent of .github/workflows/ci.yml.
// Kept alongside GitHub Actions (the CI actually enforced on this repo) to demonstrate
// Jenkinsfile/Groovy DSL fluency for orgs that run Jenkins on-prem instead of a SaaS CI.
pipeline {
    agent any

    tools {
        jdk 'temurin-21'
        nodejs 'node22'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Trivy vulnerability scan') {
            steps {
                sh '''
                    trivy fs --scanners vuln --severity CRITICAL,HIGH \
                        --exit-code 1 --ignore-unfixed --format table .
                '''
            }
        }

        stage('API — build, test, coverage') {
            steps {
                dir('api') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew build -x test --no-daemon
                        ./gradlew test --no-daemon
                        ./gradlew jacocoTestReport --no-daemon
                        ./gradlew jacocoTestCoverageVerification --no-daemon
                    '''
                }
            }
        }

        stage('Web — lint, test, build') {
            steps {
                dir('web') {
                    sh '''
                        npm ci
                        npm run lint
                        npm run test:coverage
                        npm run build
                    '''
                }
            }
        }

        stage('Web — E2E Playwright') {
            steps {
                dir('web') {
                    sh '''
                        npx playwright install --with-deps chromium
                        npm run test:e2e
                    '''
                }
            }
        }

        stage('Validate k8s manifests') {
            steps {
                sh '''
                    helm lint ./chart
                    helm template product-service ./chart --namespace product-service \
                        | kubeconform -strict -ignore-missing-schemas -summary -
                '''
            }
        }

        stage('SonarCloud analysis') {
            when {
                branch 'main'
            }
            steps {
                withSonarQubeEnv('SonarCloud') {
                    dir('api') {
                        sh './gradlew sonar --no-daemon'
                    }
                }
            }
        }

        stage('Docker build & push') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    docker build -t ghcr.io/apchavez/spring-mvc-angular-api:latest ./api
                    docker build -t ghcr.io/apchavez/spring-mvc-angular-web:latest ./web
                '''
            }
        }
    }

    post {
        always {
            junit testResults: 'api/build/test-results/**/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'api/build/reports/jacoco/test/html/**, web/playwright-report/**', allowEmptyArchive: true
        }
    }
}
