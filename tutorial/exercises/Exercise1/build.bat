rem Script to compile tutorial project

echo on

rem compile the code
set LIBPATHS=%ALP_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\build.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\alpine.jar
javac -classpath %LIBPATHS% tutorial\*.java
