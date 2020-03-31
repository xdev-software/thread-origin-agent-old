package software.xdev.tools.threadoriginagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;


/**
 * LoggerAgent add java.util.logging statements to classes at runtime using
 * <a href="http://www.javassist.org/">javassist</a>.
 * 
 * <p>
 * inspired by http://today.java.net/article/2008/04/22/add-logging-class-load-time-java-instrumentation
 * </p>
 */
public class ThreadOriginLogger implements ClassFileTransformer
{
	
	/** only this classes should instrument or leave empty to instrument all classes that not excluded */
	static final String[] INCLUDES = new String[]{
		"java/lang/Thread"
	};
	
	/** the jul logger definition */
	static final String SYN_LOGGER = "private static java.util.logging.Logger _log;";
	
	/** the jul logging if statement */
	static final String SYN_IFLOG = "if (_log.isLoggable(java.util.logging.Level.INFO))";
	
	static final String SYN_PRETTY_PRINT_METHOD =
		" private static java.lang.String _prettyStackTrace(final java.lang.StackTraceElement[] elements) {  if(elements == null || elements.length == 0)  {  return \"there were no elements on the stack\";  }  final java.lang.StringBuilder sb = new java.lang.StringBuilder();  for(final java.lang.StackTraceElement e : elements)  {  sb.append(e).append(java.lang.System.lineSeparator());  }  return sb.toString();  }";
	
	/**
	 * add agent
	 */
	public static void premain(final String agentArgument, final Instrumentation instrumentation)
	{
		instrumentation.addTransformer(new ThreadOriginLogger());
	}
	
	/**
	 * instrument class
	 */
	@Override
	public byte[] transform(
		final ClassLoader loader,
		final String className,
		final Class<?> clazz,
		final java.security.ProtectionDomain domain,
		final byte[] bytes)
	{
		System.out.println(className);
		// for(final String include : INCLUDES)
		// {
		//
		// if(className.startsWith(include))
		// {
		// return this.doClass(className, clazz, bytes);
		// }
		// }
		//
		// return this.doClass(className, clazz, bytes);
		return bytes;
		
	}
	
	/**
	 * instrument class with javasisst
	 */
	private byte[] doClass(final String name, final Class<?> clazz, byte[] b)
	{
		final ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;
		
		try
		{
			cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
			
			if(cl.isInterface() == false)
			{
				
				final CtField field = CtField.make(SYN_LOGGER, cl);
				final String getLogger = "java.util.logging.Logger.getLogger("
					+ name.replace('/', '.')
					+
					".class.getName());";
				cl.addField(field, getLogger);
				
				final CtMethod prettyPrint = CtNewMethod.make(SYN_PRETTY_PRINT_METHOD, cl);
				cl.addMethod(prettyPrint);
				
				final CtBehavior[] methods = cl.getDeclaredBehaviors();
				
				for(int i = 0; i < methods.length; i++)
				{
					
					if(methods[i].isEmpty() == false)
					{
						this.doMethod(methods[i]);
					}
				}
				
				b = cl.toBytecode();
			}
		}
		catch(final Exception e)
		{
			System.err.println("Could not instrument  " + name + ",  exception : " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			
			if(cl != null)
			{
				cl.detach();
			}
		}
		
		return b;
	}
	
	/**
	 * modify code and add log statements before the original method is called
	 * and after the original method was called
	 */
	private void doMethod(final CtBehavior method) throws NotFoundException, CannotCompileException
	{
		
		// TODO dont do this for all methods
		
		method.insertBefore(SYN_IFLOG + " _log.info(_prettyStackTrace(Thread.currentThread().getStackTrace()));");
		
	}
	
}
