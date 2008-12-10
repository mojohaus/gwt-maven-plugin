package org.codehaus.mojo.gwt.compile;

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



import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.GwtRuntime;

/**
 * Goal which compiles a GWT file.
 * 
 * @deprecated will be replaced by gwt-maven CompileMojo
 * @goal compile
 * @phase process-classes
 * @author Shinobu Kawai
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @requiresDependencyResolution compile
 */
public class CompileMojo
    extends AbstractGwtModuleMojo
{

    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File outputDirectory;

    /**
     * The level of logging detail: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL
     *
     * @parameter default-value="WARN" expression="${gwt.logLevel}"
     */
    private String logLevel;

    /**
     * Script output style: OBF[USCATED], PRETTY, or DETAILED
     *
     * @parameter default-value="OBF" expression="${gwt.style}"
     */
    private String style;

    /**
     * The directory into which generated files will be written for review
     *
     * @parameter expression="${gwt.gen}"
     */
    private File gen;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "CompileMojo#execute()" );

        Object compiler = getGwtCompilerInstance( getGwtRuntime() );

        // Replace ContextClassLoader with the classloader used to build the
        // GWTCompiler instance
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( compiler.getClass().getClassLoader() );

        // Replace the SecurityManager to intercept System.exit()
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager( new NoSystemExitSecurityManager( sm ) );
        try
        {
            for ( String module : getModules() )
            {
                compile( module, compiler );
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( cl );
            System.setSecurityManager( sm );
        }
    }

    /**
     * @param module the GWT module to compile
     * @param compiler the GWT compiler instance
     * @throws MojoExecutionException some error occured
     */
    private void compile( final String module, Object compiler )
        throws MojoExecutionException
    {
        getLog().info( "Compile GWT module " + module );
        final List < String > args = getGwtCompilerArguments( module );
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
                SystemExitSecurityException sse = (SystemExitSecurityException) e.getTargetException();
                int status = sse.getStatus();
                if ( status == 0 )
                {
                    getLog().debug( "System.exit(0) has been intercepted --> ignored" );
                }
                else
                {
                    throw new MojoExecutionException( "GWTCompiler failed Status " + status );
                }
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
    }

    /**
     * TODO : Due to PLX-220, we must convert the classpath URLs to escaped URI form. cf.
     * http://jira.codehaus.org/browse/PLX-220
     *
     * @return an alternate ClassLoader including plugin dependencies and project source foler
     * @throws MojoExecutionException failed to configure ClassLoader
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
        URL[] urls = addProjectClasspathElements( originalUrls );
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
     * Retrieve a GWTCompiler instance and configure the Thread contextClassLoader
     *
     * @return a GWTCompiler instante
     * @throws MojoExecutionException failed to retrieve an instante
     */
    protected Object getGwtCompilerInstance( GwtRuntime runtime )
        throws MojoExecutionException
    {
        // TODO : getting and invoking the main should be a more common
        // component
        final String compilerClassName = "com.google.gwt.dev.GWTCompiler";

        Object compiler = null;
        ClassLoader loader = null;
        try
        {
            loader = getClassLoader( runtime );
            compiler = loader.loadClass( compilerClassName ).newInstance();
        }
        catch ( Exception e )
        {
            try
            {
                loader = getAlternateClassLoader();
                compiler = loader.loadClass( compilerClassName );
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

    /**
     * @param module the GWT module to compile
     * @return the GWTCompiler command line arguments
     */
    protected List < String > getGwtCompilerArguments( String module )
    {
        List < String > args = new LinkedList < String > ();
        args.add( "-out" );
        args.add( outputDirectory.getAbsolutePath() );
        args.add( "-logLevel" );
        args.add( logLevel );
        args.add( "-style" );
        args.add( style );
        if ( gen != null )
        {
            args.add( "-gen" );
            args.add( gen.getAbsolutePath() );
        }
        args.add( module );
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "  Invoking main with" + args );
        }
        return args;
    }
}
