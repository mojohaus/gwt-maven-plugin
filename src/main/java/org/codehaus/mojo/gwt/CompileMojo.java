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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which compiles a GWT file.
 * 
 * @goal compile
 * @phase process-class
 * @author Shinobu Kawai
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @requiresDependencyResolution compile
 */
public class CompileMojo
    extends AbstractGwtMojo
{

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

        final List<String> args = getGwtCompilerArguments();
        Object compiler = getGwtCompilerInstance();

        // Replace ContextClassLoader with the classloader used to build the
        // GWTCompiler instance
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
     *
     * @return a ClassLoader including plugin dependencies and project source
     * foler
     * @throws MojoExecutionException failed to configure ClassLoader
     */
    private ClassLoader getClassLoader()
        throws MojoExecutionException
    {
        URLClassLoader myClassLoader = (URLClassLoader) getClass().getClassLoader();

        URL[] originalUrls = myClassLoader.getURLs();
        URL[] urls = addProjectClasspathElements( originalUrls );
        System.arraycopy( originalUrls, 0, urls, 0, originalUrls.length );

        if ( getLog().isDebugEnabled() )
        {
            for ( int i = 0; i < urls.length; i++ )
            {
                getLog().debug( "  URL:" + urls[i] );
            }
        }

        return new URLClassLoader( urls, myClassLoader.getParent() );
    }

    private URL[] addProjectClasspathElements( URL[] originalUrls )
        throws MojoExecutionException
    {
        Collection<?> sources = project.getCompileSourceRoots();
        Collection<?> resources = project.getResources();
        Collection<?> dependencies = project.getArtifacts();
        URL[] urls = new URL[originalUrls.length + sources.size() + resources.size() + dependencies.size() + 1];

        int i = originalUrls.length;
        getLog().debug( "add compile source roots to GWTCompiler classpath " + sources.size() );
        i = addClasspathElements( sources, urls, i );
        getLog().debug( "add resources to GWTCompiler classpath " + resources.size() );
        i = addClasspathElements( resources, urls, i );
        getLog().debug( "add project dependencies to GWTCompiler  classpath " + dependencies.size() );
        i = addClasspathElements( dependencies, urls, i );
        try
        {
            urls[i] = generateDirectory.toURI().toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to convert project.build.outputDirectory to URL", e );
        }
        return urls;
    }

    private int addClasspathElements( Collection<?> elements, URL[] urls, int startPosition )
        throws MojoExecutionException
    {
        for ( Object object : elements )
        {
            try
            {
                if ( object instanceof Artifact )
                {
                    urls[startPosition] = ( (Artifact) object ).getFile().toURI().toURL();
                }
                else if ( object instanceof Resource )
                {
                    urls[startPosition] = new File( ( (Resource) object ).getDirectory() ).toURI().toURL();
                }
                else
                {
                    urls[startPosition] = new File( (String) object ).toURI().toURL();
                }
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "Failed to convert original classpath element " + object + " to URL.", e );
            }
            startPosition++;
        }
        return startPosition;
    }

    /**
     * TODO : Due to PLX-220, we must convert the classpath URLs to escaped URI
     * form. cf. http://jira.codehaus.org/browse/PLX-220
     *
     * @return an alternate ClassLoader including plugin dependencies and
     * project source foler
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
     * Retrieve a GWTCompiler instance and configure the Thread
     * contextClassLoader
     *
     * @return a GWTCompiler instante
     * @throws MojoExecutionException failed to retrieve an instante
     */
    protected Object getGwtCompilerInstance()
        throws MojoExecutionException
    {
        // TODO : getting and invoking the main should be a more common
        // component
        final String compilerClassName = "com.google.gwt.dev.GWTCompiler";

        Object compiler = null;
        ClassLoader loader = null;
        try
        {
            loader = getClassLoader();
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
     * @return the GWTCompiler command line arguments
     */
    protected List<String> getGwtCompilerArguments()
    {
        List<String> args = new LinkedList<String>();
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
