pipeline {
  agent any
  stages {
    stage('Compile') {
      steps {
        script {
          def lastSuccess = currentBuild.id
          sh "echo ${lastSuccess}"
        }
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh "mvn -f joram/pom.xml clean compile"
        }
      }
    }

   stage('Unit Test') {
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh "mvn -f joram/pom.xml test"
        }
      }
    }

    stage ('Test your tests'){
      steps {
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh "mvn -f joram/pom.xml eu.stamp-project:pitmp-maven-plugin:1.3.6:descartes -DoutputFormats=HTML"
        }
         publishHTML (target: [
          allowMissing: false,
          alwaysLinkToLastBuild: false,
          keepAll: true,
          reportDir: 'joram/joram/mom/core/target/pit-reports',
          reportFiles: '**/index.html',
          reportName: "Pit Decartes"
      ])
      }
    }

    stage('Amplify') {
      when { not {branch "amplifybranch*"} 
           changeset "joram/joram/mom/core/src/test/**" }
      steps {
      script {
          dspot_test_param = "";
          def changeLogSets = currentBuild.changeSets
          for (int i = 0; i < changeLogSets.size(); i++) {
            def entries = changeLogSets[i].items
            for (int j = 0; j < entries.length; j++) {
              def entry = entries[j]
              def files = new ArrayList(entry.affectedFiles)
              for (int k = 0; k < files.size(); k++) {
                def file = files[k]
                if (file.path.endsWith("Test.java") && file.path.startsWith("joram/joram/mom/core/src/test/java")){
                  dspot_test_param += file.path.replace("joram/joram/mom/core/src/test/java/","").replace("/",".").replace(".java","") + ",";
                }
              }
            }
          }
          dspot_test_param = "-Dtest=" + dspot_test_param.substring(0, dspot_test_param.length() - 1)
        }

        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh "mvn -f joram/joram/mom/core/pom.xml eu.stamp-project:dspot-maven:amplify-unit-tests -e ${dspot_test_param}"
        }
      }
    }

    stage('Pull Request') {
      when { not {branch "amplifybranch*"}
            changeset "joram/joram/mom/core/src/test/**"
            expression { fileExists("target/dspot/output/org/")} }
      steps {
        sh 'cp -rf target/dspot/output/org/ joram/joram/mom/core/src/test/java'
        sh 'git checkout -b amplifybranch-${GIT_BRANCH}-${BUILD_NUMBER}'
        sh 'git commit -a -m "added tests"'
        // CREDENTIALID
        withCredentials([usernamePassword(credentialsId: 'github-user-password', passwordVariable: 'GITHUB_PASSWORD', usernameVariable: 'GITHUB_USER')]) {
          // REPOSITORY URL  
          sh('git push https://${GITHUB_USER}:${GITHUB_PASSWORD}@${GIT_URL} amplifybranch-${GIT_BRANCH}-${BUILD_NUMBER}')
          sh 'hub pull-request -m "Amplify pull request from build ${BUILD_NUMBER} on ${GIT_BRANCH}"'
        }
      }
    }
  }
   environment {
    GIT_URL = sh (script: 'git config remote.origin.url', returnStdout: true).trim().replaceAll('https://','')
  }
}
