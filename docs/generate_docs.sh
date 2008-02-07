#!/bin/sh
# Pipeline to generate help documents

webhome=/home/web/lappe/cmview/

# Generating HTML for web
for basename in tutorial manual installation
do
docbook2html.sh docbook/$basename.xml > web/$basename.temp
tidy web/$basename.temp > web/$basename.tidy
python web/create_cmview_web_page.py -t web/template.html -s web/$basename.tidy -o web/$basename.html
cp -f web/$basename.html $webhome
rm -f web/$basename.*
done
cp -f images/* $webhome/images/

# Generating JavaHelp
docbook2javahelp.sh docbook/manual.xml -o ../src/resources/help/
cp ../src/resources/help/index.html ../bin/resources/help/index.html

# Generating PDF documentation
for basename in tutorial manual installation
do
docbook2pdf.sh docbook/$basename.pdf pdf/$basename.pdf
done
