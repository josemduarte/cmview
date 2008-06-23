#!/usr/bin/env python
"""
create_cmview_web_pate.py -t <template> -s <source> -o <outfile>

"""

import sys
import os.path
import re
import getopt
import time

PATTERN="<!-- CONTENT -->"

    
# ---------------- main ---------------------
if __name__ == '__main__':
    try:
        opts, args = getopt.getopt(sys.argv[1:], "t:s:o:", ["template=","source=","out="])
    except getopt.GetoptError:
        sys.stderr.write("Arguments not correctly specified\n")
        # print help information and exit:
        sys.stderr.write(__doc__)
        sys.exit(2)
    template=""
    source=""
    outfile=""
    for o, a in opts:
        if o in ("-t","--template"):
            template = a
        if o in ("-s","--source"):
            source = a
        if o in ("-o","--out"):
            outfile = a
    if (template=="" or source=="" or outfile==""):
        sys.stderr.write("Missing arguments")
        sys.stderr.write(__doc__)
        sys.exit(1)
      
    flag = False
    s = open(source, "r")
    
    content=""
    lines=s.readlines()
    for line in lines:
        m=re.search("</body>",line)
        if (m):
            flag=False
        if (flag):
            content=content+line 
        m=re.search("<body>",line)
        if (m):
            flag=True
    s.close()
    
    out = open(outfile, "w")
    t = open(template,"r")
    lines=t.readlines()
    for line in lines:
        m = re.search(PATTERN,line)
        if (m):
            out.write(content)
        out.write(line)
    t.close()
    out.close()
