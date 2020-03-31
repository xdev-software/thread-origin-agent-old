# Thread Origin Agent

In many situations is it helpful to add logging functions to running code to find out which class created a **Thread**. To find out what the origin of a Thread is, we will log the stacktrace at Thread creation.

In this project we use the Java Intrumentation API and Javassist to add the
logging functionality at runtime. For this feature we manipulate the bytecode
for any class at runtime and add java.util.logging code for Thread creation.

This maven project packages all classes (incl. javassist) in a "fat jar".
This jar you can use directly to add it to the java command line: 

	java -javaagent:thread-origin-agent-1.0.0.jar

This project was inspired by https://github.com/kreyssel/maven-examples
