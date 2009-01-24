package org.codehaus.mojo.gwt.fork;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A (simplified) surefire-like bootstrapper to run some GWT component in a forked JVM without limitation of command
 * line length to define the classpath.
 * 
 * @author ndeloof
 * @version $Id$
 */
public class ForkBooter
{
    public static void main( String[] args )
        throws Exception
    {
        String fileName = args[0];
        String className = args[1];
        ClassLoader cl = getClassLoader( fileName );

        Class<?> gwt = cl.loadClass( className );
        
        Thread.currentThread().setContextClassLoader( cl );

        Method method = gwt.getMethod( "main", new Class[] { String[].class } );
        String[] compilerArgs = new String[args.length - 2];
        System.arraycopy( args, 2, compilerArgs, 0, args.length - 2 );
        List<String> compilerArgsList = Arrays.asList( compilerArgs );
        System.out.println(" running main class " + className + " with args " + compilerArgsList );
        try
        {
            method.invoke( null, (Object) compilerArgs );
        }
        catch ( Throwable e )
        {
            // to have message in the output
            e.printStackTrace();
            throw new Exception(e);
        }
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
