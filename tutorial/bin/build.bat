@echo off
SETLOCAL

if "%1" == "/h" goto :usage
if "%1" == "-h" goto :usage
if "%1" == ""   goto :usage
goto :main
:usage
echo Usage:  %0 [-v] {NUMBER}
echo.
echo /v        verbose output
echo /s        compile solution code
echo NUMBER    an exercise number (1 to 11)
echo.
echo Compile exercise source from:
echo $CIP/exercises
echo Creates a new jar:
echo $CIP/lib/exercise{NUMBER}.jar
echo.
echo Note that the solutions in "src" are compiled by the
echo "build.xml" ANT script.
echo.
echo Example:
echo # compile Exercise1 to $CIP/lib/exercise1.jar
echo %0 1
echo.
echo Example:
echo # compile Exercise1 solution to $CIP/lib/exercise1.jar with verbose output
echo %0 /v /s 1
echo.
goto :end

:main
del %COUGAAR_INSTALL_PATH%\lib\exercise*

SET CWD=%~d1%~p0

REM take VERBOSE flag
SET VERBOSE=0
if "%1" == "/v" (
  SET VERBOSE=1
  shift 1
)

REM take SOLUTION flag
SET SOLUTION=0
if "%1" == "/s" (
  SET SOLUTION=1
  shift 1
)

REM take exercise number
SET NUMBER=%1
shift 1

REM make sure the %COUGAAR_INSTALL_PATH% is set
if "%COUGAAR_INSTALL_PATH%"=="" (
  ECHO COUGAAR_INSTALL_PATH not set
  goto :end
)

REM set config options
SET JAR=%COUGAAR_INSTALL_PATH%\lib\exercise%NUMBER%.jar
if %SOLUTION%==1 (
  SET BASE=%COUGAAR_INSTALL_PATH%\tutorial\exercises\org\cougaar\tutorial
) ELSE (
  SET BASE=%COUGAAR_INSTALL_PATH%\tutorial\src\org\cougaar\tutorial
)
SET ASSET=%BASE%\assets
SET SRC=%BASE%\exercise%NUMBER%
SET TMP=%COUGAAR_INSTALL_PATH%\tutorial\tmp\exercise%NUMBER%
SET ASSET_DEF=%ASSET%\programmer_assets.def
SET PG_DEF=%ASSET%\properties.def

if not exist %SRC% (
  ECHO Unknown exercise number: %NUMBER%
  ECHO Couldn't find %SRC%
  goto :end
)

REM create property_group files
SET COMMAND= java -classpath %COUGAAR_INSTALL_PATH%\lib\core.jar;%COUGAAR_INSTALL_PATH%\clib\build.jar org.cougaar.tools.build.PGWriter %PG_DEF%
if %VERBOSE%==1 (
  echo %COMMAND%
)
cd /D %SRC%
%COMMAND%
cd /D %CWD%

REM create asset files
SET COMMAND= java -classpath %COUGAAR_INSTALL_PATH%\lib\core.jar;%COUGAAR_INSTALL_PATH%\clib\build.jar org.cougaar.tools.build.AssetWriter %PG_DEF% -Ptutorial.assets %ASSET_DEF%
if %VERBOSE%==1 (
  echo %COMMAND%
)
cd /D %SRC%
%COMMAND%
cd /D %CWD%

REM create temp directory for class files
if exist %TMP% (
  RD /q /s %TMP%
)
mkdir %TMP%

REM compile the code
SET LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\planning.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\clib\build.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\glm.jar
SET LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\servlet.jar
SET COMMAND= javac -deprecation -d %TMP% -classpath %LIBPATHS% %ASSET%\*.java %SRC%\*.java
if %VERBOSE%==1 (
  echo %COMMAND%
)
%COMMAND%

REM create jar
SET COMMAND= jar cf %JAR% -C %TMP% org
if %VERBOSE%==1 (
  echo %COMMAND%
)
%COMMAND%

if exist %COUGAAR_INSTALL_PATH%\lib\tutorial.jar (
  del %COUGAAR_INSTALL_PATH%\lib\tutorial.jar
  ECHO Warning: removed %COUGAAR_INSTALL_PATH%\lib\tutorial.jar
)

ECHO Successfully compiled %COUGAAR_INSTALL_PATH%\lib\exercise%NUMBER%.jar

:end
