rem Script to compile all tutorial files

set LIBPATHS=%ALP_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\build.jar
echo on

rem Regenerate and recompile all property/asset files
cd ..\src\alp\tutorial\computer\assets
java -classpath %LIBPATHS% alp.build.AssetWriter -Palp.tutorial.computer.assets computer_assets.def
java -classpath %LIBPATHS% alp.build.PGWriter properties.def
cd ..\..\..\..
javac -deprecation -classpath %LIBPATHS% alp\tutorial\*.java alp\tutorial\hanoi\*.java alp\tutorial\binary\*.java alp\tutorial\calendar\*.java alp\tutorial\computer\assets\*.java alp\tutorial\computer\*.java 
cd ..\bin
