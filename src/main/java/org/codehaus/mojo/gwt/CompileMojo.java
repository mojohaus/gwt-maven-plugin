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
     * @parameter expression="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File outputDirectory;

    /**
     * The GWT module to compile.
     *
     * @parameter
     * @required
     */
    private String module;

    /**
     * @parameter
     * @deprecated use 'module' parameter
     */
    private String className;

    /**
     * The level of logging detail: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or
     * ALL
     *
     * @parameter default-value="WARN"
     */
    private String logLevel;

    /**
     * Script output style: OBF[USCATED], PRETTY, or DETAILED
     *
     * @parameter default-value="OBF"
     */
    private String style;

    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "CompileMojo#execute()" );

        final List args = getGwtCompilerArguments();
        Object compiler = getGwtCompilerInstance();

        // Replace ContextClassLoader with the classloader used to build the GWTCompiler instance
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( compiler.getClass().getClassLoader() );

        // Replace the SecurityManager to intercept System.exit()
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager( new NoSystemExitSecurityManager( sm ) );
        try
        {
            getLog().debug( "invoke GWTCompiler#main(String[])" );
            Method processArgs = compiler.getClass().getMethod( "main", new Class[] { String[].class } );
            processArgs.invoke( null, new Object[] { args.toArray( new String[args.size()] ) } );
        }
        catch ( InvocationTargetException e )
        {
            if ( e.getTargetException() instanceof SystemExitSecurityException )
            {
                // System.exit has been intercepted --> ignored
            }
            else
            {
                throw new MojoExecutionException( "GWTCompiler#main(String[]) failed.", e );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "GWTCompiler#main(String[]) failed.", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( cl );
            System.setSecurityManager( sm );
        }
    }

    /**
     * Need this to run both pre- and post- PLX-220 fix.
     */
    private ClassLoader getClassLoader()
        throws MojoExecutionException
    {
        URLClassLoader myClassLoader = (URLClassLoader) getClass().getClassLoader();

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
     * TODO : Due to PLX-220, we must convert the classpath URLs to escaped URI
     * form. cf. http://jira.codehaus.org/browse/PLX-220
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

    /**
     * Retrieve a GWTCompiler instance and configure the Thread
     * contextClassLoader
     *
     * @return
     * @throws MojoExecutionException
     */
    protected Object getGwtCompilerInstance()
        throws MojoExecutionException
    {
        // TODO : getting and invoking the main should be a more common
        // component
        final String GWTCOMPILER_CLASS_NAME = "com.google.gwt.dev.GWTCompiler";

        Object compiler = null;
        ClassLoader loader = null;
        try
        {
            loader = getClassLoader();
            compiler = loader.loadClass( GWTCOMPILER_CLASS_NAME ).newInstance();
        }
        catch ( Exception e )
        {
            try
            {
                loader = getAlternateClassLoader();
                compiler = loader.loadClass( GWTCOMPILER_CLASS_NAME );
            }
            catch ( Exception ee )
            {
                throw new MojoExecutionException( "Could not find GWTCompiler.", ee );
            }
        }
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Found class:" + compiler.getClass() );
        }
        return compiler;
    }

    protected List getGwtCompilerArguments()
    {
        List args = new LinkedList();
        args.add( "-out" );
        args.add( outputDirectory.getAbsolutePath() );
        args.add( "-logLevel" );
        args.add( logLevel );
        args.add( "-style" );
        args.add( style );
        args.add( module );
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Invoking main with" + args );
        }
        return args;
    }
}
