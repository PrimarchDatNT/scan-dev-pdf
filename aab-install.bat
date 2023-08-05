@echo off
echo Install Apks
SET projectPath=./app
SET bundletool=bundletool-all-1.1.0.jar
SET buildType=v_Official_Release
java -jar %bundletool% install-apks --apks=%projectPath%\build\outputs\bundle\%buildType%\aab.apks
echo Install Apks Done