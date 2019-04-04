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
      when { changeset "joram/joram/mom/core/src/test/**" }
      steps {
      script {
          env.test_param = "";
          echo "config_changed value =  ${env.config_changed}"
          def changeLogSets = currentBuild.changeSets
          for (int i = 0; i < changeLogSets.size(); i++) {
            def entries = changeLogSets[i].items
            for (int j = 0; j < entries.length; j++) {
              def entry = entries[j]
              def files = new ArrayList(entry.affectedFiles)
              for (int k = 0; k < files.size(); k++) {
                def file = files[k]
                echo "${file.path}"
                if (file.path.endsWith(".java") || file.path.startsWith("joram/joram/mom/core/src/test/java")){
                  echo "test found"
                  env.dspot_test_param += " -Dtest="+file.path.replace("joram/joram/mom/core/src/test/java","").replace("/",".").replace(".java","");
                }
              }
            }
          }
          echo "dspot test param value =  ${env.test_param}"
        }


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
