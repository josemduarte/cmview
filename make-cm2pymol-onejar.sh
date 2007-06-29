#!/bin/sh
if [ -z "$4" ]
then
    echo "usage: make-cmview-onejar.sh <tempdir> <cmviewtag> <aglappetag> <manifest>"
    echo "if instead of a tag, you want the code from trunk, just specify 'trunk' instead of the tag name"
    exit
fi


tempdir=$1
cmviewtag=$2
aglappetag=$3
manifest=$4


cd $tempdir

if [ -e "$cmviewtag" ] || [ -e "$aglappetag" ]
then
    echo "File exists with name $cmviewtag or name $aglappetag, can't create directory"
    exit 1
fi

# exporting from svn
echo "Exporting source from svn"

if [ "$cmviewtag" = "trunk" ]
then
    cmviewtag="CM2PyMol-trunk"
    svn export file:///project/StruPPi/svn/CM2PyMol/trunk/ $cmviewtag
else
    svn export file:///project/StruPPi/svn/CM2PyMol/tags/$cmviewtag
fi

if [ "$aglappetag" = "trunk" ]
then
    aglappetag="aglappe-trunk"
    svn export file:///project/StruPPi/svn/aglappe/trunk/ $aglappetag
else
    svn export file:///project/StruPPi/svn/aglappe/tags/$aglappetag 
fi

# copying from aglappetag to cmviewtag
cp -R $aglappetag/proteinstructure $cmviewtag
cp -R $aglappetag/tools $cmviewtag
rm -rf $aglappetag

# setting classpath and compiling
CLASSPATH=.:/project/StruPPi/jars/mysql-connector-java.jar:/project/StruPPi/jars/commons-codec-1.3.jar:/project/StruPPi/jars/xmlrpc-client-3.0.jar:/project/StruPPi/jars/xmlrpc-common-3.0.jar:/project/StruPPi/jars/ws-commons-util-1.0.1.jar
echo "Compiling..."
cd $cmviewtag
javac cmview/*.java cmview/datasources/*.java

# creating jar file
echo "Creating jar file: $cmviewtag.jar ..."
jar -cfm ../$cmviewtag.jar $manifest .

# removing $cmviewtag temp directory
cd ..
rm -rf $cmviewtag