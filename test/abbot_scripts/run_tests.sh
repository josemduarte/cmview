#!/bin/sh
export CLASSPATH=$CLASSPATH:/project/StruPPi/jars/abbot.jar
java junit.extensions.abbot.ScriptFixture $@
