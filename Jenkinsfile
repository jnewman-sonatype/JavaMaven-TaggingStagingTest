// Originally from https://raw.githubusercontent.com/jnewman-sonatype/Webgoat-APR/master/Jenkinsfile
// Before you start...
// Configure ci_settings.xml "mirrorOf" and "server" to point to your nxrm with creds
// create the relevant deployment repo in NXRM
// configure Jenkins Nexus Repo and IQ connections in "Settings > Configure System"
// set "git" to auto install in "Settings > Global Tools Config"
// Configure Maven plugin install with "M3" ID in "Global Tools Config"
// I'm sure more info is missing but this is a good start.

// Install the following plugins for this script to work
// "Pipeline Utility Steps" plugin
// "Rich Text Publisher" plugin
// "Pipeline basic steps" plugin
// "user build vars" plugin
   // **IMPORTANT** you will also need to approve part of the script running from the console output. Look for this output:
      // Scripts not permitted to use method com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation getApplicationCompositionReportUrl. Administrators can decide whether to approve or reject this signature.

//Create a Jenkins pipeline build with "Project is parameterised" and declare the following string settings.
  // "iqAppID"     - DESCRIPTION: IQ Server Application ID to evaluate against
  // "iqStage"     - DESCRIPTION: IQ Server stage to evaluate against, Options are: "build | stage-release | release"
  // "DEPLOY_REPO" - DESCRIPTION: Deployment repository for your built artefact. Usually "maven-releases" or "maven-snapshots"
  // "groupId"     - DESCRIPTION: groupId taken from the project pom.xml
  // "artifactId"  - DESCRIPTION: artifactId taken from the project pom.xml
  // "version"     - DESCRIPTION: version taken from the project pom.xml
  // "packaging"   - DESCRIPTION: The file format extension of the final artefact EG "ear | war | jar"

pipeline {
    agent any
    //parameters
    // {
    //     string(name: 'groupId', defaultValue: 'JNTestApps', description: 'groupId taken from the project pom.xml')
    //     string(name: 'artifactId', defaultValue: 'JavaMaven-TaggingStagingTest', description: 'artifactId taken from the project pom.xml')
    //     string(name: 'version', defaultValue: '1.0.0', description: 'version taken from the project pom.xml')
    //     string(name: 'packaging', defaultValue: 'jar', description: 'The file format extension of the final artefact e.g. ear | war | jar')
    //     string(name: 'iqAppID', defaultValue: 'JavaMaven-TaggingStagingTest', description: 'IQ Server Application ID to evaluate against')
    //     string(name: 'iqStage', defaultValue: 'build', description: 'IQ Server stage to evaluate against, Options are: build | stage-release | release')
    //     string(name: 'DEPLOY_REPO', defaultValue: 'TSTest-Staging', description: 'Deployment repository for your built artifact. Usually maven-releases')
    // }

    environment {
        GROUP_ID = "JNTestApps"
        ARTIFACT_ID = "JavaMaven-TaggingStagingTest"
        ARTIFACT_VERSION = "1.0.0"
        PACKAGING = "jar"
        IQ_STAGE = "build"
        STAGING_DEPLOY_REPO = "TSTest-Staging"
        RELEASE_DEPLOY_REPO = "TSTest-Release"

        ARTIFACT_NAME = "${WORKSPACE}/target/${artifactId}-${version}.${packaging}"
        TAG_FILE = "${WORKSPACE}/tag.json"
        APP_ID = "${groupId}_${artifactId}"
        BUILD_VERSION = "${version}-${BUILD_NUMBER}"
        TAG_NAME = "${APP_ID}_${BUILD_VERSION}"

        IQ_SCAN_REPORT_URL = "" // defined later
    }
    tools {
       maven 'M3'
    }
    stages {

        stage('Maven Build') {
            steps {
                sh 'mvn -s ci_settings.xml -gs ci_settings.xml -B -Dproject.version=$BUILD_VERSION -Dmaven.test.failure.ignore clean package'
            }
            post {
                success {
                  echo 'Now archiving ...'
                  archiveArtifacts artifacts: "**/target/*.${PACKAGING}"
                }
            }
        }

        // Once you run this pipeline once, you will need to approve the script from the console output
        stage('Sonatype IQ/Lifecycle Scan'){
            steps {
                script{         
                    try {
                        def policyEvaluation = nexusPolicyEvaluation failBuildOnNetworkError: true, 
                        iqApplication: selectedApplication("${APP_ID}"), 
                        iqScanPatterns: [[scanPattern: "**/target/*.${packaging}"]], 
                        iqStage: "${IQ_STAGE}", 
                        jobCredentialsId: '',
                        reachability: [
                            failOnError: false,
                            timeout: '10 minutes',
                            logLevel: 'DEBUG',
                            javaAnalysis: [
                                enable: true,
                                entrypointStrategy: [
                                    $class: 'NamedStrategy',
                                    name: 'ACCESSIBLE_CONCRETE',
                                ],
                                namespaces: [
                                    [namespace: "${GROUP_ID}"]
                                ]
                                includes: []
                            ],
                            java: [
                                options:[
                                    '-Xmx4G'
                                ]
                            ]
                        ]
                        IQ_SCAN_REPORT_URL = "${policyEvaluation.applicationCompositionReportUrl}"
                        echo "Sonatype IQ/Lifecycle scan report URL: ${IQ_SCAN_REPORT_URL}"
                    } 
                    catch (error) {
                        def policyEvaluation = error.policyEvaluation
                        echo "Nexus IQ scan vulnerabilities detected', ${policyEvaluation.applicationCompositionReportUrl}"
                        throw error
                    }
                }
            }
        }

         stage('Create Nexus Repository Tag'){
            steps {
                script {
    
                    // Git data (Git plugin)
                    echo "${GIT_URL}"
                    echo "${GIT_BRANCH}"
                    echo "${GIT_COMMIT}"
                    echo "${WORKSPACE}"
                   
                    // construct the meta data (Pipeline Utility Steps plugin)
                    def tagdata = readJSON text: '{}' 
                    tagdata.buildNumber = "${BUILD_NUMBER}" as String
                    tagdata.buildId = "${BUILD_ID}" as String
                    tagdata.buildJob = "${JOB_NAME}" as String
                    tagdata.buildTag = "${BUILD_TAG}" as String
                    tagdata.appId = "${APP_ID}" as String
                    tagdata.appVersion = "${BUILD_VERSION}" as String
                    tagdata.buildUrl = "${BUILD_URL}" as String
                    tagdata.iqScanReportUrl = "${IQ_SCAN_REPORT_URL}" as String
                    tagdata.gitUrl = "${GIT_BRANCH}" as String
                    // //tagData.promote = "no" as String

                    writeJSON(file: "${TAG_FILE}", json: tagdata, pretty: 4)
                    sh 'cat ${TAG_FILE}'

                    createTag nexusInstanceId: 'nexus', tagAttributesPath: "${TAG_FILE}", tagName: "${TAG_NAME}"

                    // // write the tag name to the build page (Rich Text Publisher plugin)
                    rtp abortedAsStable: false, failedAsStable: false, parserName: 'Confluence', stableText: "Nexus Repository Tag: ${TAG_NAME}", unstableAsStable: true 
                }
            }
        }

        stage('Publish to Nexus Repository Staging Repo'){
            steps {
                script {
                    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: "${STAGING_DEPLOY_REPO}", packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: "${PACKAGING}", filePath: "${ARTiFACT_NAME}"]], mavenCoordinate: [artifactId: "${ARTIFACT_ID}", groupId: "${GROUP_ID}", packaging: "${PACKAGING}", version: "${BUILD_VERSION}"]]], tagName: "${TAG_NAME}"
                }
            }
        }
        
        stage('Simulated Staging Tests (wait 1 min)'){
            steps {
                script {
                    sleep 60 // seconds
                }
            }
        }
        
        stage('Move to Nexus Repository Release Repo'){
            steps {
                script {
                    moveComponents destination: "${RELEASE_DEPLOY_REPO}", nexusInstanceId: 'nexus', tagName: "${TAG_NAME}"
                }
            }
        }  
    }
}
