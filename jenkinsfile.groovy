node {
   def mvnHome
   stage('PREP') { 
      // Get the Maven tool.      
      mvnHome = tool 'M3'
   }
   stage('CHECKOUT SCM'){
      // get source code
      git branch: "${BRANCH}", url: 'https://github.com/patevs/mvn-ang-app.git'
   }
   stage('TEST') {
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore -Dnpm-install -Dproject-install -Dng-test clean package"
      } else {
         bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore -Dnpm-install -Dproject-install -Dng-test clean package/)
      }
   }
   stage('PUBLISH & ARCHIVE') {
      junit '**/target/surefire-reports/TEST-*.xml'
      //archiveArtifacts 'target/*.jar'
   }
}