#!/bin/Groovy/bin groovy

node {

	// BUILD JOB DETAILS
	echo "Starting build job: ${JOB_NAME} on git branch: ${BRANCH}"
	echo "Build number: ${BUILD_NUMBER}"

	// BUILD VARIABLES
	def PROJECT_DIR = "C:\\xampp-5\\htdocs"

	// Maven build tool
	def mvnHome

	// PREP STAGE
	stage('PREPARE WORKSPACE') { 
		// clean the job workspace
		deleteDir()

		// Get the Maven build tool
		mvnHome = tool 'M3'

		// copy _ss_environment config file into project folder
		configFileProvider([configFile(fileId: '_ss_environment.php', variable: 'ENV')]) {
			bat "copy /y ${ENV} ${PROJECT_DIR}\\_ss_environment.php"
		}

		bat '''
			"C:\\Program Files\\Git\\bin\\bash.exe" -c "printf \033[31mRed\033[0m"
			'''
	}

	// CHECKOUT STAGE
	stage('CHECKOUT SCM'){
		// get source code from git
		echo 'CHECKING-OUT source code from git'
		// move into project folder
		dir("${PROJECT_DIR}"){
			git branch: "${BRANCH}", url: 'https://github.com/patevs/ss_app.git'
		}
	}

	// INSTALL STAGE
	stage('INSTALL Composer Dependencies'){
		// install composer dependencies
		echo 'INSTALLING composer dependencies'
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat 'composer install --prefer-source'
		}
	}

	// BUILD STAGE
	stage('BUILD/REBUILD Database'){
		// build/rebuild mysql database
		echo 'BUILDING/REBUILDING mysql database'
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat '''
				"C:\\Program Files\\Git\\bin\\bash.exe" -c "framework/sake dev/build '' flush=all"
				'''
		}
	}

	// TEST STAGE
	stage('RUN PHPUnit Tests'){
		// running phpunit tests
		echo 'RUNNING PHPUnit tests'
		// move into project folder
		dir("${PROJECT_DIR}"){
			bat '''
				"C:\\Program Files\\Git\\bin\\bash.exe" -c "framework/sake dev/tests '' flush=all"
				'''
		}
	}

} // END OF PIPELINE

// EOF