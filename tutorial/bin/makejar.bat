rem Create tutorial jar file
REM $id$
rem
cd ..\src
jar cvf ..\lib\tutorial.jar org\cougaar\tutorial\faststart\*.class org\cougaar\tutorial\faststart\hanoi\*.class org\cougaar\tutorial\faststart\binary\*.class org\cougaar\tutorial\faststart\computer\*.class org\cougaar\tutorial\faststart\calendar\*.class org\cougaar\tutorial\faststart\computer\assets\*.class
cd ..\bin
