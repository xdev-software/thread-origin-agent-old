package software.xdev.tools.threadoriginagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;


/**
 * This javaagent should ALWAYS be the first!
 */
public class ThreadOriginTransformer implements ClassFileTransformer
{
	private static final String PROCEED = "$proceed($$); ";
	
	private static final String PRINT_STACK =
		"java.lang.StackTraceElement[] elements = java.lang.Thread.currentThread().getStackTrace(); "
			+
			"java.lang.StringBuilder sb = new java.lang.StringBuilder(); "
			+
			"for(int i=0; i<elements.length; i++) "
			+
			"   sb.append(\"\\t\").append(elements[i]).append(java.lang.System.lineSeparator()); "
			+
			"java.lang.System.out.println(sb.toString()); ";
	
	/**
	 * Don't log calls to classnames if they start with the string mentioned here<br/>
	 * e.g: -javaagent:thread-origin-agent-1.0.0.jar=sun/awt,sun/java2d
	 */
	private final List<String> excluded = new ArrayList<>();
	
	public ThreadOriginTransformer(final String argument)
	{
		super();
		
		System.out.println("Arg: " + argument);
		
		if(argument != null)
		{
			this.excluded.addAll(Arrays.asList(argument.split(",")));
		}
		
		System.out.println("Ignoring excluded: " + String.join(",", this.excluded));
	}
	
	/**
	 * add agent
	 * 
	 * @see src/main/resources/META-INF/MAINFEST.MF
	 */
	public static void premain(final String agentArgument, final Instrumentation instrumentation)
	{
		instrumentation.addTransformer(new ThreadOriginTransformer(agentArgument));
		
		System.out.println("Trying to retransform loaded classes");
		long failed = 0;
		long success = 0;
		for(final Class<?> loadedClazz : instrumentation.getAllLoadedClasses())
		{
			if(loadedClazz.getName().startsWith("software.xdev.tools")
				|| loadedClazz.getName().startsWith("javassist"))
			{
				System.out.println("Ignoring " + loadedClazz.getName());
				continue;
			}
			
			try
			{
				instrumentation.retransformClasses(loadedClazz);
				success++;
			}
			catch(final UnmodifiableClassException e)
			{
				failed++;
			}
			
		}
		System.out.println("Retransform loaded classes; " + success + "x successful, " + failed + "x failed");
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
		if(className == null)
		{
			System.out.println("ClassName was null; Class=" + clazz.getCanonicalName());
			return resultingBytes;
		}
		
		if(this.excluded.stream().anyMatch(className::startsWith))
		{
			System.out.println("Excluded class=" + className);
			return resultingBytes;
		}
		
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
						// System.out.println("Could not find method '" + m.getSignature() + "':" + e.getMessage());
						return;
					}
					
					final String classname = method.getDeclaringClass().getName();
					final String methodName = method.getName();
					
					ThreadOriginTransformer.this.replaceThread(classname, m, methodName);
				}
			});
			
			resultingBytes = classUnderTransformation.toBytecode();
			
		}
		catch(final Exception e)
		{
			System.out.println(
				"Could not instrument "
					+ className
					+ "/"
					+ clazz.getCanonicalName()
					+ ", exception: "
					+ e.getMessage());
		}
		
		return resultingBytes;
	}
	
	void replaceThread(final String classname, final MethodCall m, final String methodName)
		throws CannotCompileException
	{
		if(!Thread.class.getName().equals(classname))
		{
			return;
		}
		
		if(methodName.equals("start"))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			sb.append(
				"System.out.println(\"Detected Thread.start() id: \" + ((Thread)$0).getId() + \" name: \" + ((Thread)$0).getName()); ");
			sb.append(PRINT_STACK);
			sb.append(PROCEED);
			sb.append("} ");
			
			m.replace(sb.toString());
		}
		else if(methodName.equals("join"))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			sb.append(
				"System.out.println(\"Detected Thread.join() id: \" + ((Thread)$0).getId() + \" name: \" + ((Thread)$0).getName()); ");
			sb.append(PROCEED);
			sb.append("} ");
			
			m.replace(sb.toString());
		}
	}
	
}
