@echo OFF
REM $id$

if "%ALP_INSTALL_PATH%"=="" goto AIP_ERROR
if "%1"=="" goto ARG_ERROR

set LIBPATHS=%ALP_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\xml4j_2_0_11.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\alpine.jar
set LIBPATHS=.;%LIBPATHS%

REM pass in "NodeName" to run a specific named Node

set MYPROPERTIES=
set MYMEMORY=
set MYCLASSES=alp.society.Node
set MYARGUMENTS= -c -n "%1"

@ECHO ON

java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %2 %3
goto QUIT

:AIP_ERROR
echo Please set ALP_INSTALL_PATH
goto QUIT

:ARG_ERROR
echo Run requires an argument  eg: Run BinaryNode or BinarySimpleNode
goto QUIT

:QUIT
