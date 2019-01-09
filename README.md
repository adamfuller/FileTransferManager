# FileTransferManager
Java program for transferring files between computers on a network

# Compiling to Jar
First compile all .java files to .class files
```
javac *.java
```
Then compile the .class files into a .jar file
```
jar cfm FileTransferManager.jar manifest.txt *.class
```
# Executing Jar
```
java -jar FileTransferManager.jar
```
