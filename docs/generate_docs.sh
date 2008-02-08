#!/bin/sh
# Pipeline to generate help documents

webhome=/home/web/lappe/cmview/

# Generating HTML for web
echo Generating web pages...
for basename in tutorial manual installation
do
docbook2html.sh docbook/$basename.xml > web/$basename.temp
tidy web/$basename.temp > web/$basename.tidy
python web/create_cmview_web_page.py -t web/template.html -s web/$basename.tidy -o web/$basename.html
cp -f web/$basename.html $webhome
rm -f web/$basename.*
done
echo Updating website...
cp -f images/* $webhome/images/

# Generating JavaHelp
echo Generating JavaHelp...
docbook2javahelp.sh docbook/manual.xml -o ../src/resources/help/
cp -f ../src/resources/help/* ../bin/resources/help/

# Generating PDF documentation
echo Generating PDF documentation...
for basename in tutorial manual installation
do
docbook2pdf.sh docbook/$basename.xml pdf/$basename.pdf
done
