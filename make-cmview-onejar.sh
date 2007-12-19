#!/bin/sh
if [ -z "$3" ]
then
    echo "usage: make-cmview-onejar.sh <tempdir> <cmviewtag> <aglappetag>"
    echo "if instead of a tag, you want the code from trunk, just specify 'trunk' instead of the tag name"
    exit
fi


tempdir=$1
cmviewtag=$2
aglappetag=$3

CLASSPATH=.:/project/StruPPi/jars/mysql-connector-java.jar:\
/project/StruPPi/jars/commons-codec-1.3.jar:\
/project/StruPPi/jars/xmlrpc-client-3.0.jar:/project/StruPPi/jars/xmlrpc-common-3.0.jar:\
/project/StruPPi/jars/ws-commons-util-1.0.1.jar:\
/project/StruPPi/jars/vecmath.jar:\
/project/StruPPi/jars/Jama-1.0.2.jar:\
/project/StruPPi/jars/jaligner.jar:\
/project/StruPPi/jars/java-getopt-1.0.13.jar:\
/project/StruPPi/jars/collections-generic-4.01.jar:\
/project/StruPPi/jars/jung/jung-api-2.0-alpha2.jar:\
/project/StruPPi/jars/jung/jung-graph-impl-2.0-alpha2.jar

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
    cmviewtag="CMView-trunk"
    svn export file:///project/StruPPi/svn/CMView/trunk/ $cmviewtag
else
    svn export file:///project/StruPPi/svn/CMView/tags/$cmviewtag
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
cp -R $aglappetag/sadp $cmviewtag
cp -R $aglappetag/actionTools $cmviewtag
rm -rf $aglappetag

# compiling
echo "Compiling..."
cd $cmviewtag
javac -classpath $CLASSPATH cmview/*.java cmview/datasources/*.java cmview/toolUtils/*.java cmview/sadpAdapter/*.java

# creating jar file
echo "Creating jar file: $cmviewtag-StruPPi.jar ..."
jar -cfm ../$cmviewtag-StruPPi.jar Manifest-StruPPi.txt .
echo "Creating jar file: $cmviewtag-MacWin.jar ..."
jar -cfm ../$cmviewtag-MacWin.jar Manifest.txt .

# removing $cmviewtag temp directory
cd ..
rm -rf $cmviewtag
