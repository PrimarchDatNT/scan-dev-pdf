@echo off
echo Build Apks
SET projectPath=./app
SET bundletool=bundletool-all-1.1.0.jar
SET buildType=v_Official_Release
java -jar %bundletool% build-apks --bundle=%projectPath%/build/outputs/bundle/%buildType%/obfuscated-final.aab --output=%projectPath%/build/outputs/bundle/%buildType%/aab.apks --ks=%projectPath%/keystore/keystore.jks --ks-pass=pass:"#y+jP-N2pgf@<VU" --ks-key-alias=Scano2020 --key-pass=pass:"#y+jP-N2pgf@<VU"
echo Build Apks done