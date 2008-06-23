#!/bin/sh
cmviewdir=`dirname $0`
cd $cmviewdir
java -Xmx256m -jar CMView.jar $*