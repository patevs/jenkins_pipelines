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
		// clean the workspace
		deleteDir()
		// Get the Maven tool.      
		mvnHome = tool 'M3'
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
			bat "${GIT_BASH} -c framework/sake dev/build '' flush=all"
		}
	}

} // END OF PIPELINE

// EOF