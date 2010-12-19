#!/bin/sh
if [ -z "$1" ]
then
	echo "Usage: docbook2javahelp infile.xml [options]"
	exit
fi

infile=$1
shift 1

xsltproc $@ /project/StruPPi/Software/docbook/docbook-xsl-1.73.2/javahelp/javahelp.xsl $infile
