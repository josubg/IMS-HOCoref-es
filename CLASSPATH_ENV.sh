#!/usr/bin/bash

##
## The classpath for java. You don't need to change this.
##

#CLASSPATH="ims-hotcoref-standalone.jar"

CLASSPATH="ims-hotcoref.jar"
for jar in `dirname ${BASH_SOURCE[0]}`/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$jar
done

export CLASSPATH
