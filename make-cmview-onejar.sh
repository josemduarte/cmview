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
/project/StruPPi/jars/xmlrpc-client-3.1.jar:/project/StruPPi/jars/xmlrpc-common-3.1.jar:\
/project/StruPPi/jars/ws-commons-util-1.0.2.jar:\
/project/StruPPi/jars/vecmath.jar:\
/project/StruPPi/jars/Jama-1.0.2.jar:\
/project/StruPPi/jars/jaligner.jar:\
/project/StruPPi/jars/java-getopt-1.0.13.jar:\
/project/StruPPi/jars/collections-generic-4.01.jar:\
/project/StruPPi/jars/jung/jung-api-2.0-alpha2.jar:\
/project/StruPPi/jars/jung/jung-graph-impl-2.0-alpha2.jar:\
/project/StruPPi/jars/jh.jar


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
    svn export file:///project/StruPPi/svn/CMView/trunk/src $cmviewtag
else
    svn export file:///project/StruPPi/svn/CMView/tags/$cmviewtag/src $cmviewtag
fi

if [ "$aglappetag" = "trunk" ]
then
    aglappetag="aglappe-trunk"
    svn export file:///project/StruPPi/svn/aglappe/trunk/ $aglappetag
    # special for CMView distribution: putting a cleaned up contactTypes.dat:
	svn export file:///project/StruPPi/svn/CMView/trunk/test/config_files/contactTypes.dat $aglappetag/proteinstructure/contactTypes.dat 
else
    svn export file:///project/StruPPi/svn/aglappe/tags/$aglappetag
    # special for CMView distribution: putting a cleaned up contactTypes.dat:
	svn export file:///project/StruPPi/svn/CMView/tags/$cmviewtag/test/config_files/contactTypes.dat $aglappetag/proteinstructure/contactTypes.dat     
fi


# copying from aglappetag to cmviewtag
cp -R $aglappetag/proteinstructure $cmviewtag
mkdir $cmviewtag/tools
cp $aglappetag/tools/MySQLConnection.java $aglappetag/tools/PymolServerOutputStream.java $cmviewtag/tools
cp -R $aglappetag/sadp $cmviewtag
cp -R $aglappetag/actionTools $cmviewtag
rm -rf $aglappetag

# adding license headers
for file in `find $cmviewtag -name "*.java"`
do
	echo -e "$license" > $file.tmp
	cat $file >> $file.tmp
	mv -f $file.tmp $file
done

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
