rem Script to compile tutorial project

echo on

rem compile the code
set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\build.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\glm.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\planserver.jar
javac -classpath %LIBPATHS% tutorial\*.java
