javac *.java;
jar cfm FileTransferManager.jar manifest.txt *.class;
rm File\ Transfer\ Manager.app/Contents/Java/FileTransferManager.jar;
cp FileTransferManager.jar  File\ Transfer\ Manager.app/Contents/Java/FileTransferManager.jar;