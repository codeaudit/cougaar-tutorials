rem Script to compile all tutorial files

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\build.jar
echo on

rem Regenerate and recompile all property/asset files
cd ..\src\org\cougaar\tutorial\faststart\computer\assets
java -classpath %LIBPATHS% org.cougaar.tools.build.AssetWriter -Porg.cougaar.tutorial.faststart.computer.assets computer_assets.def
java -classpath %LIBPATHS% org.cougaar.tools.build.PGWriter properties.def
cd ..\..\..\..\..\..
javac -deprecation -classpath %LIBPATHS% org\cougaar\tutorial\faststart\*.java org\cougaar\tutorial\faststart\hanoi\*.java org\cougaar\tutorial\faststart\binary\*.java org\cougaar\tutorial\faststart\calendar\*.java org\cougaar\tutorial\faststart\computer\assets\*.java org\cougaar\tutorial\faststart\computer\*.java 
cd ..\bin
