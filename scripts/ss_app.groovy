#!/bin/Groovy/bin groovy

node {

	// BUILD JOB DETAILS
	echoGreen("Starting build job: ${JOB_NAME} on git branch: ${BRANCH}")
	echoGreen("Build number: ${BUILD_NUMBER}")

	// BUILD FOLDERS
	def PROJECT_DIR = "C:\\xampp-5\\htdocs"
	def REPORTS_DIR = "${WORKSPACE}\\test-reports"

	// Maven build tool
	def mvnHome

	// PREP STAGE
	stage('PREPARE WORKSPACE') { 
		// clean the job workspace
		deleteDir()
		// ensure the reports folder exists
		bat "mkdir ${REPORTS_DIR}"

		// Get the Maven build tool
		mvnHome = tool 'M3'

		// copy _ss_environment config file into project folder
		configFileProvider([configFile(fileId: '_ss_environment.php', variable: 'ENV')]) {
			bat "copy /y ${ENV} ${PROJECT_DIR}\\_ss_environment.php"
		}
	}

	// CHECKOUT STAGE
	stage('CHECKOUT SCM'){
		// checkout source code from git
		echoGreen("CHECKING-OUT source code from git")
		// move into project folder
		dir("${PROJECT_DIR}"){
			git branch: "${BRANCH}", url: 'https://github.com/patevs/ss_app.git'
		}
	}

	// INSTALL STAGE
	stage('INSTALL Composer Dependencies'){
		// install composer dependencies
		echoGreen("INSTALLING composer dependencies")
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat 'composer install --prefer-source'
		}
	}

	// DEPENDENCY CHECK STAGE
	stage('CHECK Dependency Version'){
		// check for outdated composer dependencies
		echoGreen("Checking for outdated composer dependencies")
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat "vendor\\bin\\dependensees >> ${REPORTS_DIR}\\dependensees-report.txt"
			// composer show -i (short for --installed) or composer [global] show -i -t short for --tree
		}
	}

	// BUILD STAGE
	stage('BUILD/REBUILD Database'){
		// build/rebuild mysql database
		echoGreen("BUILDING/REBUILDING MySQL Database")
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat '''
				:: "C:\\Program Files\\Git\\bin\\bash.exe" -c "framework/sake dev/build '' flush=all"
				'''
		}
	}

	// TEST STAGE
	stage('RUN PHPUnit Tests'){
		// running phpunit tests
		echoGreen("RUNNING PHPUnit tests")
		// move into project folder
		dir("${PROJECT_DIR}"){
			// run php unit tests
			bat '''
				:: vendor/bin/phpunit "" siteconfig/tests "" db=mysql flush=all
				'''
			bat '''
				:: "C:\\Program Files\\Git\\bin\\bash.exe" -c "framework/sake dev/tests/all '' flush=all"
				'''
			bat '''
				:: "C:\\Program Files\\Git\\bin\\bash.exe" -c "find **/tests/ -name '*Test.php'"
				"C:\\Program Files\\Git\\bin\\bash.exe" -c "ls -d **/tests/"
				'''
			bat '''
				:: vendor/bin/fastest --help
				:: ("C:\\Program Files\\Git\\bin\\bash.exe" -c "ls -d siteconfig/tests/" | vendor\\bin\\fastest "vendor\\bin\\phpunit --testdox-html assets\\phpunit-report\\report.html \"\" {} \"\" db=mysql flush=all") || (exit 0)
				:: ("C:\\Program Files\\Git\\bin\\bash.exe" -c "ls -d **/tests/" | vendor\\bin\\fastest "vendor\\bin\\phpunit --testdox-html assets\\phpunit-reports\\report.html \"\" {} \"\" db=mysql flush=all") || (exit 0)
				:: vendor\\bin\\fastest -x phpunit.xml -vvv --no-ansi -n "vendor\\bin\\phpunit {}"
				'''
			bat """
				("C:\\Program Files\\Git\\bin\\bash.exe" -c "ls -d siteconfig/tests/" | vendor\\bin\\fastest -v -n "vendor\\bin\\phpunit --testdox-html assets\\phpunit-report\\phpunit-report.html \"\" {} \"\" db=mysql flush=all") || (exit 0)
				"""
		}
	}

	// ARCHIVE & PUBLISH STAGE
	stage('ARCHIVE & PUBLISH Build Reports'){
		// archiving build reports
		echoGreen("ARCHIVING build reports")
		// move into project folder
		dir("${PROJECT_DIR}"){
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'phpunit-report', reportFiles: 'phpunit-report.html', reportName: 'PHPUnit Report', reportTitles: ''])
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'build/dependensees/', reportFiles: 'index.html', reportName: 'Dependencies Report', reportTitles: ''])
		}
		// move into reports folder
		dir("${REPORTS_DIR}"){
			archiveArtifacts '**'
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: '', reportFiles: 'dependensees-report.txt', reportName: 'Dependencies Summary', reportTitles: ''])
		}
	}

} // END OF PIPELINE
 
// output green 
def echoGreen(text){
	ansiColor('xterm') {
		echo "\033[32m${text}\033[0m"
	}
}

// EOF