echo off
REM $id$
rem Recompile all tutorials 

if "%COUGAAR_INSTALL_PATH%"=="" goto AIP_ERROR

call clearall
call compileall
call makejar
goto QUIT

:AIP_ERROR
echo Please set COUGAAR_INSTALL_PATH
goto QUIT

:QUIT
