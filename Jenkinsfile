@Library('stamp') _

pipeline {
  agent any
  stages {
    stage('Compile') {
      steps {
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

    stage ('Test your tests with Descartes'){
      //when {
      //  changeset "joram/joram/mom/core/src/test/**"
      //}
      steps {
        sh "echo 'Test case change detected, start to assess them with PitMP/Descartes...'"
        withMaven(maven: 'maven3', jdk: 'JDK8') {
          sh "mvn -f joram/pom.xml eu.stamp-project:pitmp-maven-plugin:descartes -DoutputFormats=HTML"
        }
        sh "echo 'Test case change detected, assessment with Pit finished, publishing HTML report...'"
        publishHTML (target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: true,
            reportDir: 'joram/joram/mom/core/target/pit-reports',
            reportFiles: '2020*/index.html',
            reportName: "Mutation coverage"
        ])
      }
    }

    stage('Amplify tests with DSpot') {
     when { not {branch "amplifybranch*"} 
          changeset "joram/joram/mom/core/src/main/**" }
      steps {
        sh "echo 'Code change detected, start to amplify test cases with DSpot...'"
        // script {
        //    dspot_test_param = "";
        //    def changeLogSets = currentBuild.changeSets
        //    for (int i = 0; i < changeLogSets.size(); i++) {
        //      def entries = changeLogSets[i].items
        //      for (int j = 0; j < entries.length; j++) {
        //        def entry = entries[j]
        //        def files = new ArrayList(entry.affectedFiles)
        //        for (int k = 0; k < files.size(); k++) {
        //           def file = files[k]
        //           echo 'current file: ' + file.path
        //           if (file.path.endsWith("Test.java") && file.path.startsWith("joram/joram/mom/core/src/test/java")) {
        //             echo file.path + ' selected for amplification'
        //             dspot_test_param += file.path.replace("joram/joram/mom/core/src/test/java/","").replace("/",".").replace(".java","") + ",";
        //           }
        //         }
        //       }
        //     }
        //     echo 'building input tests for DSpot with: ' + dspot_test_param
        //    //dspot_test_param = "-Dtest=" + dspot_test_param.substring(0, dspot_test_param.length() - 1)
        //  }

         withMaven(maven: 'maven3', jdk: 'JDK8') {
            dir ("joram/joram/mom/core") {
            sh "mvn eu.stamp-project:dspot-maven:amplify-unit-tests -Dverbose -Diteration=4"
          }
        }
      }
    }

    stage('Pull Request') {
        when { not {branch "amplifybranch*"}
            //changeset "joram/joram/mom/core/src/test/**"
            expression { fileExists("joram/joram/mom/core/target/dspot/output/org")} }
      steps {
        sh 'cp -rf joram/joram/mom/core/target/dspot/output/org/ joram/joram/mom/core/src/test/java'
        sh 'git checkout -b amplifybranch-${GIT_BRANCH}-${BUILD_NUMBER}'
        sh 'git commit -a -m "added tests"'
        // CREDENTIALID
        withCredentials([usernamePassword(credentialsId: 'github-token', passwordVariable: 'GITHUB_PASSWORD', usernameVariable: 'GITHUB_USER')]) {
          // REPOSITORY URL  
          sh('git push https://${GITHUB_USER}:${GITHUB_PASSWORD}@${GIT_URL} amplifybranch-${GIT_BRANCH}-${BUILD_NUMBER}')
//          sh 'hub pull-request -m "Amplify pull request from build ${BUILD_NUMBER} on ${GIT_BRANCH}"'
          script {
            stamp.pullRequest("${GITHUB_PASSWORD}", "joram", "STAMP-project", "amplify Test", "amplify Test Build Number ${GIT_BRANCH}-${BUILD_NUMBER}", "amplifybranch-${GIT_BRANCH}-${BUILD_NUMBER}", "${GIT_BRANCH}")
          }
        }
      }
    }
  }
   environment {
    GIT_URL = sh (script: 'git config remote.origin.url', returnStdout: true).trim().replaceAll('https://','')
  }
}
