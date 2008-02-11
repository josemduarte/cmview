#!/bin/sh
# Pipeline to generate CMView help documents from docbook sources
# 11/Feb/2008, stehr@molgen.mpg.de

webhome=/home/web/lappe/cmview/

# Generating HTML for web
echo Generating web pages...
for basename in tutorial manual installation faq screenshots download index
do
echo $basename
docbook2html.sh docbook/$basename.xml > web/$basename.temp
tidy web/$basename.temp > web/$basename.tidy
python web/create_cmview_web_page.py -t web/template.html -s web/$basename.tidy -o web/$basename.html
done

# Generating JavaHelp
echo Generating JavaHelp...
docbook2javahelp.sh docbook/manual.xml -o ../src/resources/help/
cp -f ../src/resources/help/* ../bin/resources/help/

# Generating PDF documentation
echo Generating PDF documentation...
for basename in tutorial manual installation
do
echo $basename
docbook2pdf.sh docbook/$basename.xml pdf/$basename.pdf
done
