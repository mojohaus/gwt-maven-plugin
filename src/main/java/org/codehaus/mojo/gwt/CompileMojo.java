package org.codehaus.mojo.gwt;

/*
 * Copyright 2006- org.codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which compiles a GWT file.
 *
 * @goal compile
 * @phase compile
 * @author Shinobu Kawai
 */
public class CompileMojo
    extends AbstractMojo
{

    /**
     * Location of the source files.
     *
     * @parameter expression="${basedir}/src/main/java"
     * @required
     */
    private File sourceDirectory;

    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}/gwt/www"
     * @required
     */
    private File outputDirectory;

    /**
     * The java class to compile.
     *
     * @parameter
     * @required
     */
    private String className;

    public void execute()
        throws MojoExecutionException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "CompileMojo#execute()" );
        }

        // TODO : getting and invoking the main should be a more common component
        final String GWTCOMPILER_CLASS_NAME = "com.google.gwt.dev.GWTCompiler";

        getLog().debug( "  COMPILER: " + GWTCOMPILER_CLASS_NAME );

        ClassLoader loader = null;
        Class compiler = null;
        try
        {
            loader = getClassLoader();
            compiler = loader.loadClass( GWTCOMPILER_CLASS_NAME );
        }
        catch ( ClassNotFoundException e )
        {
            try
            {
                loader = getAlternateClassLoader();
                compiler = loader.loadClass( GWTCOMPILER_CLASS_NAME );
            }
            catch( ClassNotFoundException ee )
            {
                throw new MojoExecutionException( "Could not find GWTCompiler.", ee );
            }
        }
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Found class:" + compiler );
        }

        final Method main;

        try
        {
            main = compiler.getMethod( "main", new Class[] { String[].class } );
        }
        catch ( SecurityException e )
        {
            throw new MojoExecutionException( "Permission not granted for reflection.", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoExecutionException( "Could not find GWTCompiler#main(String[]).", e );
        }
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Found method:" + main );
        }

        // TODO : what other options are there?
        final List args = new LinkedList();
        args.add( "-out" );
        args.add( outputDirectory.getAbsolutePath() );
        args.add( className );
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Invoking main with" + args );
        }

        // TODO : can we have the gwt source directory already in the classpath?
        Runnable compile = new Runnable()
        {
            public void run()
            {
                try
                {
                    main.invoke( null, new Object[] { args.toArray( new String[args.size()] ) } );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new RuntimeException( "This shouldn't happen.", e );
                }
                catch ( IllegalAccessException e )
                {
                    throw new RuntimeException( "Permission not granted for reflection.", e );
                }
                catch ( InvocationTargetException e )
                {
                    throw new RuntimeException( "GWTCompiler#main(String[]) failed.", e );
                }
            }
        };

        // TODO : we can just swap ContextClassLoader in this block
        Thread compileThread = new Thread( compile );
        compileThread.setContextClassLoader( loader );
        compileThread.start();
        try
        {
            compileThread.join();
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "Compiler thread stopped.", e );
        }
    }

    /**
     * Need this to run both pre- and post- PLX-220 fix.
     */
    private ClassLoader getClassLoader()
        throws MojoExecutionException
    {
        URLClassLoader myClassLoader = (URLClassLoader)getClass().getClassLoader();

        URL[] originalUrls = myClassLoader.getURLs();
        URL[] urls = new URL[originalUrls.length + 1];
        System.arraycopy( originalUrls, 0, urls, 0, originalUrls.length );

        try
        {
            urls[originalUrls.length] = sourceDirectory.toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to convert source root to URL.", e );
        }

        if ( getLog().isDebugEnabled() )
        {
            for ( int i = 0; i < urls.length; i++ )
            {
                getLog().debug( "  URL:" + urls[i] );
            }
        }
    
        return new URLClassLoader( urls, myClassLoader.getParent() );
    }

    /**
     * TODO : Due to PLX-220, we must convert the classpath URLs to escaped URI form.
     * cf. http://jira.codehaus.org/browse/PLX-220
     */ 
    private ClassLoader getAlternateClassLoader()
        throws MojoExecutionException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "CompileMojo#getAlternateClassLoader()" );
        }

        URLClassLoader myClassLoader = (URLClassLoader) getClass().getClassLoader();

        URL[] originalUrls = myClassLoader.getURLs();
        URL[] urls = new URL[originalUrls.length + 1];
        for ( int index = 0; index < originalUrls.length; ++index )
        {
            try
            {
                String url = originalUrls[index].toExternalForm();
                urls[index] = new File( url.substring( "file:".length() ) ).toURI().toURL();
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "Failed to convert original classpath to URL.", e );
            }
        }

        // TODO : can we have the gwt source directory already in the classpath?
        try
        {
            urls[originalUrls.length] = sourceDirectory.toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to convert source root to URL.", e );
        }

        if ( getLog().isDebugEnabled() )
        {
            for ( int i = 0; i < urls.length; i++ )
            {
                getLog().debug( "  URL:" + urls[i] );
            }
        }

        return new URLClassLoader( urls, myClassLoader.getParent() );
    }
}
