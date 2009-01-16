package org.codehaus.mojo.gwt.fork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

/**
 * A (simplified) surefire-like bootstrapper to run some GWT component in a forked JVM without limitation of command
 * line length to define the classpath.
 * 
 * @author ndeloof
 */
public class ForkBooter
{
    public static void main( String[] args )
        throws Exception
    {
        String fileName = args[0];
        String className = args[1];
        ClassLoader cl = getClassLoader( fileName );

        Class gwt = cl.loadClass( className );

        Thread.currentThread().setContextClassLoader( cl );
        Method method = gwt.getMethod( "main", new Class[] { String[].class } );
        String[] compilerArgs = new String[args.length - 1];
        System.arraycopy( args, 1, compilerArgs, 0, args.length - 1 );
        method.invoke( null, (Object) compilerArgs );
    }

    private static ClassLoader getClassLoader( String fileName )
        throws FileNotFoundException, IOException, MalformedURLException
    {
        BufferedReader reader = new BufferedReader( new FileReader( fileName ) );
        List<URL> classpath = new LinkedList<URL>();
        String line;
        while ( ( line = reader.readLine() ) != null )
        {
            classpath.add( new File( line ).toURI().toURL() );
        }
        URL[] urls = (URL[]) classpath.toArray( new URL[classpath.size()] );
        return new URLClassLoader( urls, ClassLoader.getSystemClassLoader() );
    }
}
