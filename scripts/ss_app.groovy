#!/bin/Groovy/bin groovy

node {

	// BUILD JOB DETAILS
	echoBoldBlue("Starting build job: ${JOB_NAME} on git branch: ${BRANCH}")
	echoBoldBlue("Build number: ${BUILD_NUMBER}")

	// BUILD FOLDERS
	def PROJECT_DIR = "C:\\xampp-5\\htdocs"
	def REPORTS_DIR = "${WORKSPACE}\\test-reports"

	// PREP STAGE
	stage('PREPARE WORKSPACE') { 
		prepWS("${PROJECT_DIR}", "${REPORTS_DIR}")
	}

	// CHECKOUT STAGE
	stage('CHECKOUT SCM'){
		checkoutSCM("${PROJECT_DIR}", "${BRANCH}")
	}

	// INSTALL STAGE
	stage('INSTALL Composer Dependencies'){
		installDependencies("${PROJECT_DIR}")
	}

	// DEPENDENCY CHECK STAGE
	stage('SHOW Outdated Dependencies'){
		// check for outdated composer dependencies
		echoGreen("SHOWING outdated composer dependencies")
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat "composer show -o"
		}
	}

	// BUILD STAGE
	stage('BUILD/REBUILD Database'){
		// build/rebuild mysql database
		echoGreen("BUILDING/REBUILDING MySQL Database")
		// move into project folder
		dir("${PROJECT_DIR}"){
			withEnv(['GIT_BASH="C:\\Program Files\\Git\\bin\\bash.exe"']) {
				bat '''
					%GIT_BASH% -c "framework/sake dev/build '' flush=all"
					'''
			}
		}
	}

	// TEST STAGE
	stage('RUN PHPUnit Tests'){
		// running phpunit tests
		echoGreen("RUNNING PHPUnit tests")
		// move into project folder
		dir("${PROJECT_DIR}"){
			// run php unit tests
			withEnv(['GIT_BASH="C:\\Program Files\\Git\\bin\\bash.exe"',"REPORTS_DIR=${REPORTS_DIR}\\phpunit-report"]) {
				bat '''
					%GIT_BASH% -c "ls -d siteconfig/tests/" | fastest -vvv -n "vendor\\bin\\phpunit --testdox-html %REPORTS_DIR%\\phpunit-report.html --log-junit %REPORTS_DIR%\\phpunit-report.xml {}" || (exit 0) 
					'''
			}
			//bat 'fastest -x phpunit.xml -vvv -n "vendor\\bin\\phpunit {}" || (exit 0)'
		}
	}

	// QA STAGE
	stage('RUN QA Tools'){
		echoGreen("RUNNING QA Tools")
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat 'phpqa tools'
			// run php unit tests
			//--analyzedDirs mysite
			bat "phpqa --ignoredDirs cms,framework,reports,siteconfig,themes,vendor --tools phpmetrics --buildDir ${REPORTS_DIR}\\qa-reports --report offline -n -v"
		}
	}

	// ARCHIVE & PUBLISH STAGE
	stage('ARCHIVE & PUBLISH Build Reports'){
		// archiving build reports
		echoGreen("ARCHIVING build reports")
		// move into reports folder
		dir("${REPORTS_DIR}"){
			archiveArtifacts '**'
			junit 'phpunit-report/*.xml'
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'qa-reports', reportFiles: 'phpqa.html', reportName: 'QA Report', reportTitles: ''])
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'qa-reports/phpmetrics', reportFiles: '**.html', reportName: 'phpmetrics Report', reportTitles: ''])
			publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'phpunit-report', reportFiles: '*.html', reportName: 'PHPUnit Report', reportTitles: ''])
		}
	}

} // END OF PIPELINE

// prepare the job workspace
def prepWS(project, reports){
	echoBoldGreen("Preparing Job Workspace")
	// clean the job workspace
	deleteDir()
	// ensure the reports folder exists
	bat "mkdir ${reports}"
	// copy _ss_environment config file into project folder
	configFileProvider([configFile(fileId: '_ss_environment.php', variable: 'ENV')]) {
		bat "copy /y ${ENV} ${project}\\_ss_environment.php"
	}
}

// checkout source from SCM
def checkoutSCM(project, branch){
	echoBoldGreen("PULLING source code from git")
	// move into project folder
	dir("${project}"){
		git branch: "${branch}", url: 'https://github.com/patevs/ss_app.git'
	}
}

// install composer dependencies
def installDependencies(project){
	echoGreen("INSTALLING composer dependencies")
	// move into project folder
	dir("${project}"){
		bat 'composer install --prefer-source'
	}
}

// define colors
//def RED   = "\033[0;31m"
//def GREEN = "\033[0;32m"
//def BLUE  = "\e[34m"
//def CYAN  = "\e[36m"
 
// output red 
def echoRed(text){
	ansiColor('xterm') {
		echo "\033[0;31m${text}\033[0m"
	}
}
def echoBoldRed(text){
	ansiColor('xterm') {
		echo "\033[1;31m${text}\033[1m"
	}
}

// output green 
def echoGreen(text){
	ansiColor('xterm') {
		echo "\033[32m${text}\033[0m"
	}
}
def echoBoldGreen(text){
	ansiColor('xterm') {
		echo "\033[1;32m${text}\033[1m"
	}
}

// output blue
def echoBlue(text){
	ansiColor('xterm') {
		echo "\033[36m${text}\033[0m"
	}
}
def echoBoldBlue(text){
	ansiColor('xterm') {
		echo "\033[1;36m${text}\033[1m"
	}
}

// EOF