#!/bin/sh
# Publish CMView webpages
# Note: HTML and image files have to exist in web/ and images/ folders

webhome=/home/web/lappe/cmview/

# Generating HTML for web
echo Copying web pages...
for basename in tutorial manual installation faq screenshots download index
do
echo $basename
cp -f web/$basename.html $webhome
#rm -f web/$basename.*
done
echo Copying images and css...
cp -f images/* $webhome/images/
cp -f web/*.css $webhome
