rem Script to compile tutorial project

echo on

rem Regenerate and recompile all property/asset files
call makeassets

rem compile the code
set LIBPATHS=%ALP_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\build.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\alpine.jar
javac -deprecation -classpath %LIBPATHS% tutorial\*.java tutorial\assets\*.java
