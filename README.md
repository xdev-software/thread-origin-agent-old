> [!WARNING]
> This repo was migrated to https://github.com/xdev-software/thread-origin-agent

# Thread Origin Agent

In many situations is it helpful to add logging functions to running code to find out which class created a **Thread**. To find out what the origin of a Thread is, we will log the stacktrace at Thread creation.

In this project we use the Java Intrumentation API and Javassist to add the
logging functionality at runtime. For this feature we manipulate the bytecode
for any class at runtime and add java.util.logging code for Thread creation.

This maven project packages all classes (incl. javassist) in a "fat jar".
This jar you can use directly to add it to the java command line: 

	java -javaagent:thread-origin-agent-1.0.0.jar

This project was inspired by https://github.com/kreyssel/maven-examples

## Usage

* Build the project with Maven: ``mvn clean install``
  * ``thread-origin-agent-1.0.0.jar`` should be generated in the ``target`` directory
* Insert ``-javaagent:<pathTothread-origin-agent-1.0.0.jar>=<packagesToIgnore>`` as far forward as possible into the JVM-arguments
  * Examples:
    *  ``java -jar <programToInspect>.jar -javaagent:thread-origin-agent-1.0.0.jar``
    *  ``java -jar <programToInspect>.jar -javaagent:"C:\temp\thread-origin-agent-1.0.0.jar"=sun/awt,sun/java2d``
