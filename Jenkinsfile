pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh '''cd joram
	  mvn install'''
        }
      }
    }
    stage('Amplify') {
//      when { changeset "joram/joram/mom/core/src/test/**" }
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh '''cd joram
          mvn eu.stamp-project:dspot-maven:amplify-unit-tests -e '''
      }
        sh 'cp -rf joram/target/dspot/output/org/ joram/joram/mom/core/src/test/java'
      }
    }

  }
   environment {
    GIT_URL = sh (script: 'git config remote.origin.url', returnStdout: true).trim().replaceAll('https://','')
  }
}
