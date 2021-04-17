pipeline {
    agent any
    // Triggers for calling builds
    triggers {
        githubPush()
    }
    // Additional credentials for gradle tasks
    environment {
        NEXUS = credentials("NachtRaben-Nexus")
    }
    // Options to configure workspace
    options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()
    }
    // Tools to specify specific gradle/jdk/etc tools
    tools {
        gradle 'latest'
        jdk 'JDK-11'
    }
    stages {
        // Test code can compile successfully
        stage ('Build') {
            steps {
                sh 'gradle clean shadowJar publish'
            }
        }
        // Save the build artifacts for automatic deployment
        stage ('Archive') {
            steps {
                echo "Grabbing artifacts..."
                archiveArtifacts artifacts: '**/build/libs/*.jar', onlyIfSuccessful: true
            }
        }
    }
}