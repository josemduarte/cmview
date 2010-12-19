#!/bin/sh
if [ -z "$1" ]
then
	echo "Usage: docbook2html infile.xml > outfile.html"
	exit
fi
xsltproc /project/StruPPi/Software/docbook/docbook-xsl-1.73.2/xhtml/docbook.xsl $@  
