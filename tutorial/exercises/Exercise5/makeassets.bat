rem Script to generate asset classes

set LIBPATHS=%ALP_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\lib\build.jar
echo on

rem Regenerate and recompile all property/asset files
cd tutorial\assets
java -classpath %LIBPATHS% alp.build.AssetWriter properties.def -Ptutorial.assets programmer_assets.def
java -classpath %LIBPATHS% alp.build.PGWriter properties.def
cd ..\..
