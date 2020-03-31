package software.xdev.tools.threadoriginagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;


/**
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
	
	// static final String SYN_PRETTY_PRINT_METHOD =
	// "static java.lang.String _prettyStackTrace(java.lang.StackTraceElement[] elements) { if(elements == null ||
	// elements.length == 0) return \"there were no elements on the stack\" ; java.lang.StringBuilder sb = new
	// java.lang.StringBuilder(); for(final java.lang.StackTraceElement e : elements)
	// sb.append(e).append(java.lang.System.lineSeparator()); return sb.toString(); } ";
	
	static final String SYN_PRETTY_PRINT_METHOD =
		"static java.lang.String _prettyStackTrace(java.lang.StackTraceElement[] elements) {   java.lang.StringBuilder sb = new java.lang.StringBuilder();  return sb.toString(); } ";
	
	// static final String SYN_PRETTY_PRINT_METHOD =
	// "static java.lang.String _prettyStackTrace(java.lang.StackTraceElement payload) { return \"hans\" + payload; }";
	
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
		
		byte[] resultingBytes = bytes;
		try
		{
			
			final ClassPool classPool = ClassPool.getDefault();
			final CtClass classUnderTransformation = classPool.makeClass(new java.io.ByteArrayInputStream(bytes));
			
			if(classUnderTransformation == null)
			{
				return resultingBytes;
			}
			
			classUnderTransformation.instrument(new ExprEditor()
			{
				
				@Override
				public void edit(final MethodCall m) throws CannotCompileException
				{
					CtMethod method = null;
					try
					{
						method = m.getMethod();
					}
					catch(final NotFoundException e)
					{
						System.out.println(e.getMessage());
						return;
					}
					final String classname = method.getDeclaringClass().getName();
					final String methodName = method.getName();
					if(classname.equals(Thread.class.getName())
						&& methodName.equals("start"))
					{
						// // ThreadOriginLogger.this.addLoggingInfrastructure(classUnderTransformation);
						// m.replace(
						// "{ System.out.println(\"Detected thread starting with id: \" + ((Thread)$0).getId() );
						// java.lang.StackTraceElement[] elements = java.lang.Thread.currentThread().getStackTrace();
						// java.lang.StringBuilder sb = new java.lang.StringBuilder(); for(java.lang.StackTraceElement e
						// : elements) sb.append(e).append(java.lang.System.lineSeparator());
						// java.lang.System.out.println(sb.toString()); $proceed($$); } ");
						//
						m.replace(
							"{ System.out.println(\"Detected thread starting with id: \" + ((Thread)$0).getId() + \" name: \" + ((Thread)$0).getName() ); java.lang.StackTraceElement[] elements = java.lang.Thread.currentThread().getStackTrace(); java.lang.StringBuilder sb = new java.lang.StringBuilder();   for(int i=0; i<elements.length; i++)   sb.append(elements[i]).append(java.lang.System.lineSeparator());   java.lang.System.out.println(sb.toString()); $proceed($$); } ");
						
					}
					else if(classname.equals(Thread.class.getName())
						&& methodName.equals("join"))
					{
						m.replace(
							"{ System.out.println(\"Detected thread joining with id: \" + ((Thread)$0).getId());  $proceed($$); } ");
					}
					
				}
			});
			
			resultingBytes = classUnderTransformation.toBytecode();
		}
		catch(final Exception e)
		{
			System.err.println("Could not instrument  " + className + ",  exception : " + e.getMessage());
			e.printStackTrace();
		}
		
		return resultingBytes;
	}
	
	private void addLoggingInfrastructure(final CtClass classUnderTransformation)
	{
		if(classUnderTransformation.isInterface() == false)
		{
			try
			{
				classUnderTransformation.getDeclaredMethod("_prettyStackTrace");
			}
			catch(final NotFoundException e)
			{
				System.out.println(e.getMessage() + " <-- so we are adding it");
				try
				{
					final CtMethod prettyPrint = CtNewMethod.make(SYN_PRETTY_PRINT_METHOD, classUnderTransformation);
					classUnderTransformation.addMethod(prettyPrint);
				}
				catch(final CannotCompileException e2)
				{
					System.err.println("Could not add _prettyStackTrace method");
					e2.printStackTrace();
				}
			}
			
		}
	}
	
}
