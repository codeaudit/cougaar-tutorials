rem Create tutorial jar file
REM $id$
rem
cd ..\src
jar cvf ..\lib\tutorial.jar alp\tutorial\*.class alp\tutorial\hanoi\*.class alp\tutorial\binary\*.class alp\tutorial\computer\*.class alp\tutorial\calendar\*.class alp\tutorial\computer\assets\*.class
cd ..\bin
