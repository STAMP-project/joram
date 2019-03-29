pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh ''cd joram
	  mvn compile''
        }
      }
    }
    stage('Unit Tests') {
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh ''cd joram
          mvn test''
        }
        junit(testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true)
      }
    }
    stage('Amplify') {
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh ''cd joram
          mvn eu.stamp-project:dspot-maven:amplify-unit-tests -e''
        }
        sh 'tree joram/target/dspot/output'
      }
    }

  }
   environment {
    GIT_URL = sh (script: 'git config remote.origin.url', returnStdout: true).trim().replaceAll('https://','')
  }
}
