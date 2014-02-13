#!/bin/sh
cmviewdir=`dirname $0`

java -Xmx256m -cp $cmviewdir/../lib/uber-CMView*.jar cmview.Start $*

