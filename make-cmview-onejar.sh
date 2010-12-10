#!/bin/sh
if [ -z "$3" ]
then
    echo "usage: make-cmview-onejar.sh <tempdir> <path-to-jars> <cmviewtag> <owltag>"
    echo "if instead of a tag, you want the code from trunk, just specify 'trunk' instead of the tag name. Path-to-jars could be /project/StruPPi/jars"
    exit
fi


tempdir=$1
classp=$2
cmviewtag=$3
owltag=$4

# set here the root of svn repository for cmview
svnroot="svn://www.bioinformatics.org/svnroot/cmview"

# set here the root of svn repository for owl
owlroot="svn://www.bioinformatics.org/svnroot/owl"

# set here the directory where the 'jar' executable is located (should be the JDK bin directory)
jar_cmd_path=/usr/bin

# compile with Java6 (MPIMG specific setting)
JAVAVERSION=1.6.0 

# set here all jars which are required for building owl/cmview
CLASSPATH=.:$classp/mysql-connector-java-5.0.5-bin.jar:\
$classp/vecmath.jar:\
$classp/commons-codec-1.3.jar:\
$classp/drmaa.jar:\
$classp/jaligner.jar:\
$classp/Jama-1.0.2.jar:\
$classp/java-getopt-1.0.13.jar:\
$classp/jh.jar:\
$classp/jJRclient-RE817.jar:\
$classp/jung/jung-algorithms-2.0-beta1.jar:\
$classp/jung/jung-api-2.0-beta1.jar:\
$classp/jung/jung-graph-impl-2.0-beta1.jar:\
$classp/ws-commons-util-1.0.2.jar:\
$classp/xmlrpc-client-3.1.jar:\
$classp/xmlrpc-common-3.1.jar:\
$classp/uniprot/aopalliance.jar:\
$classp/uniprot/commons-httpclient.jar:\
$classp/uniprot/commons-logging.jar:\
$classp/uniprot/log4j.jar:\
$classp/uniprot/spring-aop.jar:\
$classp/uniprot/sprint-beans.jar:\
$classp/uniprot/spring-core.jar:\
$classp/uniprot/spring-remoting.jar:\
$classp/uniprot/uniprotjapi.jar:\
$classp/jung/collections-generic-4.01.jar

echo $CLASSPATH

# this license header will be added to each source file
license="/*\n\
***************************************************************************\n\
*   Copyright (C) 2008 Structural Proteomics Group, Max Planck Institute  *\n\
*   for Molecular Genetics, Berlin, Germany                               *\n\
*                                                                         *\n\
*   This program is free software; you can redistribute it and/or modify  *\n\
*   it under the terms of the GNU General Public License as published by  *\n\
*   the Free Software Foundation; either version 2 of the License, or     *\n\
*   (at your option) any later version.                                   *\n\
*                                                                         *\n\
*   This program is distributed in the hope that it will be useful,       *\n\
*   but WITHOUT ANY WARRANTY; without even the implied warranty of        *\n\
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *\n\
*   GNU General Public License for more details.                          *\n\
*                                                                         *\n\
*   You should have received a copy of the GNU General Public License     *\n\
*   along with this program; if not, write to the                         *\n\
*   Free Software Foundation, Inc.,                                       *\n\
*   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *\n\
***************************************************************************\n\
*/"


cd $tempdir

if [ -e "$cmviewtag" ] || [ -e "$owltag" ]
then
    echo "File exists with name $cmviewtag or name $owltag, can't create directory"
    exit 1
fi

# exporting from svn
echo "Exporting source from svn"

if [ "$cmviewtag" = "trunk" ]
then
    cmviewtag="CMView-trunk"
    svn export $svnroot/trunk/src $cmviewtag
else
    svn export $svnroot/tags/$cmviewtag/src $cmviewtag
fi

if [ "$owltag" = "trunk" ]
then
    owltag="owl-trunk"
    svn export $owlroot/trunk/ $owltag
    # special for CMView distribution: putting a cleaned up contactTypes.dat:
	svn export $svnroot/trunk/test/config_files/contactTypes.dat $owltag/core/structure/contactTypes.dat 
else
    svn export $owlroot/tags/$owltag
    # special for CMView distribution: putting a cleaned up contactTypes.dat:
	svn export $svnroot/tags/$cmviewtag/test/config_files/contactTypes.dat $owltag/core/structure/contactTypes.dat     
fi


# copying from owltag to cmviewtag
cp $owltag/src/owl/core/features/*.java $cmviewtag
cp $owltag/src/owl/core/runners/*.java  $cmviewtag
cp $owltag/src/owl/core/runners/blast/*.java   $cmviewtag
cp $owltag/src/owl/core/runners/tinker/*.java   $cmviewtag
cp $owltag/src/owl/core/sequence/Sequence.java   $cmviewtag
cp $owltag/src/owl/core/sequence/alignment/*.java   $cmviewtag
cp $owltag/src/owl/core/structure/*.java   $cmviewtag
cp $owltag/src/owl/core/structure/alignment/*.java    $cmviewtag
cp $owltag/src/owl/core/structure/features/*.java   $cmviewtag
cp $owltag/src/owl/core/structure/graphs/*.java    $cmviewtag
cp $owltag/src/owl/core/util/*.java   $cmviewtag
cp $owltag/src/owl/core/util/actionTools/*.java   $cmviewtag
cp $owltag/src/owl/deltaRank/*.java   $cmviewtag
cp $owltag/src/owl/sadp/*.java   $cmviewtag
cp $owltag/src/owl/embed/*.java   $cmviewtag
cp $owltag/src/owl/embed/contactmaps/*.java   $cmviewtag
cp $owltag/src/owl/gmbp/*.java   $cmviewtag
cp $owltag/src/owl/graphAveraging/*.java   $cmviewtag
cp $owltag/src/owl/core/connections/*.java $cmviewtag


cp -R $owltag/src/owl/core/connections/* $cmviewtag
#cp -R $owltag/src/owl/core/features/* $cmviewtag
#cp -R $owltag/src/owl/core/runners/* $cmviewtag
#cp -R $owltag/src/owl/core/runners/blast/* $cmviewtag
#cp -R $owltag/src/owl/core/runner/gromacs/* $cmviewtag
#cp -R $owltag/src/owl/core/runners/tinker/* $cmviewtag
#cp -R $owltag/src/owl/core/sequence/Sequence.java $cmviewtag
#cp -R $owltag/src/owl/core/sequence/alignment/* $cmviewtag
#cp -R $owltag/src/owl/core/structure/* $cmviewtag
#cp -R $owltag/src/owl/core/structure/alignment/* $cmviewtag
#cp -R $owltag/src/owl/core/structure/features/* $cmviewtag
#cp -R $owltag/src/owl/core/structure/graphs/* $cmviewtag
#cp -R $owltag/src/owl/core/structure/scoring/* $cmviewtag
#cp -R $owltag/src/owl/core/util/* $cmviewtag
#cp -R $owltag/src/owl/core/util/actionTools/* $cmviewtag
#cp -R $owltag/src/owl/deltaRank/* $cmviewtag
#cp -R $owltag/src/owl/sadp/* $cmviewtag
#cp -R $owltag/src/owl/embed/* $cmviewtag
#cp -R $owltag/src/owl/embed/contactmaps/* $cmviewtag
#cp -R $owltag/src/owl/gmbp/* $cmviewtag
#cp -R $owltag/src/owl/graphAveraging/* $cmviewtag
#mkdir $cmviewtag/tools
#cp $owltag/src/owl/core/util/MySQLConnection.java $cmviewtag/tools
#cp -R $owltag/src/owl/sadp $cmviewtag
#cp -R $owltag/actionTools $cmviewtag
#rm -rf $owltag

# adding license headers
for file in `find $cmviewtag -name "*.java"`
do
	echo "$license" > $file.tmp
	cat $file >> $file.tmp
	mv -f $file.tmp $file
done

# compiling
echo "Compiling..."
cd $cmviewtag
# ../$owltag/src/owl/core/structure/scoring/*.java 
rm  ../$owltag/src/owl/core/util/R.java 
rm  ../$owltag/src/owl/core/connections/UniProtConnection.java
/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Commands/javac -classpath $CLASSPATH *.java cmview/*.java cmview/datasources/*.java cmview/gmbp/*.java cmview/jpredAdapter/*.java cmview/tinkerAdapter/*.java  

#javac -classpath $CLASSPATH cmview/Start.java cmview/*.java cmview/datasources/*.java cmview/toolUtils/*.java cmview/sadpAdapter/*.java cmview/*/*.java *.java cmview/gmbp/*.java
# creating jar file
echo "Creating jar file: $cmviewtag-StruPPi.jar ..."
#/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Commands/jar -cfm ../$cmviewtag-StruPPi.jar Manifest-StruPPi.txt .
$jar_cmd_path/jar -cfm ../$cmviewtag-StruPPi.jar Manifest-StruPPi.txt .
echo "Creating jar file: $cmviewtag-MacWin.jar ..."
#/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Commands/jar -cfm ../$cmviewtag-MacWin.jar Manifest.txt .
$jar_cmd_path/jar -cfm ../$cmviewtag-MacWin.jar Manifest.txt .

# removing $cmviewtag temp directory
cd ..
#rm -rf $cmviewtag
