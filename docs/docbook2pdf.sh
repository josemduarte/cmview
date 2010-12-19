#!/bin/sh
if [ -z "$2" ]
then
	echo "Usage: $0 infile.xml outfile.pdf"
	exit
fi

infile=$1
outfile=$2

xsltproc --stringparam paper.type "A4" --stringparam toc.section.depth 2 --stringparam ulink.show "0" /project/StruPPi/Software/docbook/docbook-xsl-1.73.2/fo/docbook.xsl $infile > temp.fo
fop temp.fo $outfile
rm -f temp.fo

