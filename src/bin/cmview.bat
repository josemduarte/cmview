@ECHO OFF
REM note: this has not been tested since maven introduction! 
REM should work only executing from bin/ directory

java -Xmx256m -cp ../lib/uber-CMView*.jar cmview.Start %1 %2 %3 %4 %5 %6