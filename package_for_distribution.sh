#!/bin/sh

# this script must be executed in same directory as it is on
if [ -z "$4" ]
then
	echo "usage: $0 <tmp dir> <cmview jar> <jars dir> <version>"
	exit 1
fi

roottmpdir=$1
cmviewjar=$2
jarsdir=$3
ver=$4


# files
tmpdir=$roottmpdir/cmview-$ver
mkdir $tmpdir
cp $cmviewjar $tmpdir/CMView.jar
cp cmview.sh $tmpdir
cp cmview.bat $tmpdir
cp README $tmpdir
cp LICENSE $tmpdir
cp gpl.txt $tmpdir
cp cmview.cfg $tmpdir
# jars
mkdir $tmpdir/jars
cp $jarsdir/*.jar $tmpdir/jars

# zipping up
cd $roottmpdir
zip -r cmview-$ver.zip cmview-$ver/