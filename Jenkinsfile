node {
  def login

  try {
    notifyBuild('STARTED')
    def sbtHome = tool 'sbt'

    stage('Pull Latest From Github'){
      git branch: 'master', url: 'https://github.com/asciiu/fomo.git'
    }

    stage('Building'){
      ansiColor('xterm') {
        sh "${sbtHome}/bin/sbt clean"
        sh "${sbtHome}/bin/sbt project assembly"
      }
    }
  } catch (e) {
    currentBuild.result = "FAILED"
    throw e
  } finally {
    notifyBuild(currentBuild.result)
  }
}


def notifyBuild(String buildStatus = 'STARTED') {

  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
}
