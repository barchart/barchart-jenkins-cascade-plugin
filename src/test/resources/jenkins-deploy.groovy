/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */

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
