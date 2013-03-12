
#
# automatic self-deploy on the development jenkins server 
#

println "############"

def env = System.getenv()

def JENKINS_HOME= env["JENKINS_HOME"]
def WORKSPACE = env["WORKSPACE"]

println "JENKINS_HOME=$JENKINS_HOME"
println "WORKSPACE=$WORKSPACE"

def source = new File( "$WORKSPACE/target/maven-release-cascade.hpi" )
def target = new File( "$JENKINS_HOME/plugins/maven-release-cascade.jpi" )

target.delete()
target << source.bytes

println "############"
