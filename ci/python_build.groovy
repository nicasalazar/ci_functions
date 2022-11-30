def call(dockerRepoName, imageName, portNum) {
    pipeline {
        agent any
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name:'DEPLOY')
            
        stages {
            stage("Build") {
                steps {
                    sh "pip install -r requirements.txt"
                }
            }
            stage("Python Lint") {
                steps {
                    sh "pylint-fail-under --fail_under 2.0 *.py"
                }
            }
            stage("Package") {
                when {
                    expression { env.GIT_BRANCH == "origin/main" }
                }
                steps {
                    withCredentials([string(credentialsId: ${{ secrets.DOCKER_HUB_USERNAME }}, variable: ${{ secrets.DOCKER_HUB_ACCESS_TOKEN }})]) {
                        sh "docker login -u 'nicasalazar7' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag nicasalazar7/${dockerRepoName}:${imageName} ."
                        sh "docker push nicasalazar7/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage("Zip Artifacts") {
                steps {
                    script{
                        sh "zip pythonzip.zip *.py"
                        archiveArtifacts artifacts: '**/*.zip'
                        }
                }
            }
            stage("Deliver") {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sh "docker stop ${dockerRepoName} || true && docker rm ${dockerRepoName} || true"
                    sh "docker run -d -p ${portNum}:${portNum} --name ${dockerRepoName} ${dockerRepoName}:latest"

                }
            }
        }
    }
}
