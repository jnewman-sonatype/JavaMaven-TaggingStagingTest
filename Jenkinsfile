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
/*    parameters
    {
        string(name: 'groupId', description: 'groupId from the project pom.xml')
        string(name: 'artifactId', description: 'artifactId from the project pom.xml')
        string(name: 'version', description: 'version from the project pom.xml')
        string(name: 'packaging', description: 'The file format extension of the final artefact e.g. ear | war | jar')
        string(name: 'iqAppID', description: 'IQ Server Application ID to evaluate against')
        string(name: 'iqStage', description: 'IQ Server stage to evaluate against, Options are: build | stage-release | release')
        // string(name: 'nexusInstanceId', description: 'Nexus Repository Manager InstanceId as defined in Settings, Configure System')
        string(name: 'DEPLOY_REPO', description: 'Deployment repository for your built artifact. Usually maven-releases')
    } */
    environment {
       ARTEFACT_NAME = "${WORKSPACE}/target/${artifactId}-${version}.${packaging}"
       //DEPLOY_REPO = 'maven-releases'
       TAG_FILE = "${WORKSPACE}/tag.json"
       IQ_SCAN_URL = ""
       iqStage = "${iqStage}"
    }
    tools {
       maven 'M3'
    }
    stages {
      
        stage('Setup parameters') {
            steps {
                script { 
                    properties([
                        parameters([
                            string(
                                name: 'groupId',
                                description: 'groupId from the project pom.xml'
                            ),
                            string(
                                name: 'artifactId',
                                description: 'artifactId from the project pom.xml'
                            ),
                            string(
                                name: 'version',
                                description: 'version from the project pom.xml'
                            ),
                            string(
                                name: 'packaging',
                                description: 'The file format extension of the final artefact e.g. ear | war | jar',
                                defaultValue: 'jar'
                            ),
                            string(
                                name: 'iqAppID',
                                description: 'IQ Server Application ID to evaluate against'
                            ),
                            string(
                                name: 'iqStage',
                                description: 'IQ Server stage to evaluate against, Options are: build | stage-release | release',
                                defaultValue: 'build'
                            ),
                            string(
                                name: 'DEPLOY_REPO',
                                description: 'Deployment repository for your built artifact. Usually maven-releases',
                                defaultValue: 'maven-releases'
                            )
                        ])
                    ])
                }
            }
        }
      
/*
        stage('POM Evaluation')
        {
            steps
            {
                script
                {
                    String pomGroupId = sh script: 'mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout', returnStdout: true
                    String pomArtifactId = sh script: 'mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout', returnStdout: true
                    String pomVersion = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
                    String pomPackaging = sh script: 'mvn help:evaluate -Dexpression=project.packaging -q -DforceStdout', returnStdout: true
                    echo "POM Defined GAV: ${pomGroupId} ${pomArtifactId} ${pomVersion}"
                    echo "POM Defined packaging: ${pomPackaging}"
                }
            }
        }
*/
        stage('Build') {
            steps {
                sh 'mvn -s ci_settings.xml -gs ci_settings.xml -B -Dproject.version=$BUILD_VERSION -Dmaven.test.failure.ignore clean package'
            }
            post {
                success {
                  echo 'Now archiving ...'
                  archiveArtifacts artifacts: "**/target/*.${packaging}"
                }
            }
        }

        // Once you run this pipeline once, you will need to approve the script from the console output
        stage('Nexus IQ Scan'){
            steps {
                script{         
                    try {
                        def policyEvaluation = nexusPolicyEvaluation failBuildOnNetworkError: true, 
                        iqApplication: selectedApplication("${iqAppID}"), 
                        iqScanPatterns: [[scanPattern: "**/target/*.${packaging}"]], 
                        iqStage: "${iqStage}", 
                        jobCredentialsId: '',
                        callflow: [
                          enable: true,
                          failOnError: false,
                          timeout: '10 minutes',
                          logLevel: 'DEBUG',
                          entrypointStrategy: [
                            $class: 'NamedStrategy',
                            name: 'ACCESSIBLE_CONCRETE',
                            namespaces: [
                              'JNTestApps'
                            ]
                          ],
                          includes: [],
                          java: [
                            options:[
                              '-Xmx4G'
                            ]
                          ]
                        ]
                        echo "Nexus IQ scan succeeded: ${policyEvaluation.applicationCompositionReportUrl}"
                        IQ_SCAN_URL = "${policyEvaluation.applicationCompositionReportUrl}"
                    } 
                    catch (error) {
                        def policyEvaluation = error.policyEvaluation
                        echo "Nexus IQ scan vulnerabilities detected', ${policyEvaluation.applicationCompositionReportUrl}"
                        throw error
                    }
                }
            }
        }

/*         stage('Create tag'){
            steps {
                script {
    
                    // // Git data (Git plugin)
                    echo "${GIT_URL}"
                    echo "${GIT_BRANCH}"
                    echo "${GIT_COMMIT}"
                    echo "${WORKSPACE}"

                    
                    // // construct the meta data (Pipeline Utility Steps plugin)
                    def tagdata = readJSON text: '{}' 
                    tagdata.buildNumber = "${BUILD_NUMBER}" as String
                    tagdata.buildId = "${BUILD_ID}" as String
                    tagdata.buildJob = "${JOB_NAME}" as String
                    tagdata.buildTag = "${BUILD_TAG}" as String
                    tagdata.appVersion = "${version}" as String
                    tagdata.buildUrl = "${BUILD_URL}" as String
                    tagdata.iqScanUrl = "${IQ_SCAN_URL}" as String
                    tagdata.gitUrl = "${GIT_BRANCH}" as String
                    // //tagData.promote = "no" as String

                    writeJSON(file: "${TAG_FILE}", json: tagdata, pretty: 4)
                    sh 'cat ${TAG_FILE}'

                    createTag nexusInstanceId: 'nexus', tagAttributesPath: "${TAG_FILE}", tagName: "${BUILD_TAG}"

                    // // write the tag name to the build page (Rich Text Publisher plugin)
                    rtp abortedAsStable: false, failedAsStable: false, parserName: 'Confluence', stableText: "Nexus Repository Tag: ${BUILD_TAG}", unstableAsStable: true 
                }
            }
        }

        stage('Upload to Nexus Repository'){
            steps {
                script {
                    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: "${DEPLOY_REPO}", packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: "${packaging}", filePath: "${ARTEFACT_NAME}"]], mavenCoordinate: [artifactId: "${artifactId}", groupId: "${groupId}", packaging: "${packaging}", version: "${version}"]]], tagName: "${BUILD_TAG}"
                }
            }
        } */
    }
}
