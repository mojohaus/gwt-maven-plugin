package org.codehaus.mojo.gwt;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.Commandline;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Execute the i18nCreator tool
 * 
 * @deprecated will be replaced with gw-maven I18NMojo
 * @goal i18n
 * @phase generate-sources
 * @see http://code.google.com/webtoolkit/documentation/com.google.gwt.doc.DeveloperGuide.Fundamentals.html#i18nCreator
 * @author ndeloof
 */
public class I18NCreatorMojo
    extends AbstractGwtMojo
{
    /**
     * The messages ResourceBundles used to generate the GWT i18n inteface
     *
     * @parameter
     */
    private String[] resourceBundles;

    /**
     * Timeout for i18nCreator execution in a dedicated JVM
     * @parameter default-value="10000"
     */
    private int timeOut;

    /**
     * Shortcut for a single resourceBundle
     * 
     * @parameter
     */
    @SuppressWarnings( "unused" )
    private String resourceBundle;

    /**
     * Maven Hack : resourceBundle attribute is used to declare the parameter, but plexus will use
     * the setter to inject value.
     * @param resourceBundle the single bundle to process
     */
    public void setResourceBundle( String resourceBundle )
    {
        this.resourceBundles = new String[] { resourceBundle };
    }

    /**
     * If true, create scripts for a ConstantsWithLookup interface rather than a Constants one
     * 
     * @parameter expression="gwt.createConstantsWithLookup"
     */
    private boolean constantsWithLookup;

    /**
     * If true, create scripts for a Messages interface rather than a Constants one
     *
     * @parameter default-value="true" expression="gwt.createMessages"
     */
    private boolean messages;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        for ( String bundle : resourceBundles )
        {
            runI18NSync( bundle );
        }
    }

    /**
     * @param bundle the message bundle to convert to i18n interface
     * @throws MojoExecutionException some error occured
     */
    private void runI18NSync( String bundle )
        throws MojoExecutionException
    {
        getLog().info( "Running I18NSync to generate message bundles from " + bundle );

        String jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        List < String > classpath = new ArrayList < String > ();
        classpath.addAll( getProject().getCompileSourceRoots() );
        List < Resource > resources = getProject().getResources();
        for ( Resource resource : resources )
        {
            classpath.add( resource.getDirectory() );
        }

        URL[] pluginClasspath = ( (URLClassLoader) getClass().getClassLoader() ).getURLs();
        for ( URL url : pluginClasspath )
        {
            classpath.add( url.getFile() );
        }

        try
        {
            // We need to fork a process as I18NSync uses the system classloader to load the resourceBundle,
            // so we have no option to reconfigure the classloader (as CompileMojo does)
            // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2894

            Commandline cli = new Commandline();
            cli.setExecutable( jvm );
            cli.createArg( false ).setLine( "-classpath" );
            cli.createArg( false ).setLine( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            cli.createArg( false ).setLine( "com.google.gwt.i18n.tools.I18NSync" );
            cli.createArg( false ).setLine( "-out" );
            File file = new File( generateDirectory, bundle.replace( '.', File.separatorChar ) );
            file.getParentFile().mkdirs();
            cli.createArg( false ).setLine( generateDirectory.getAbsolutePath() );
            if ( constantsWithLookup )
            {
                cli.createArg( false ).setLine( "-createConstantsWithLookup" );
            }
            if ( messages )
            {
                cli.createArg( false ).setLine( "-createMessages" );
            }
            cli.createArg( false ).setLine( bundle );

            getLog().debug( "execute : " + cli.toString() );
            StreamConsumer systemOut = new ForkingStreamConsumer( new StandardOutputConsumer() );
            StreamConsumer systemErr = new ForkingStreamConsumer( new StandardOutputConsumer() );
            int status = CommandLineUtils.executeCommandLine( cli, systemOut, systemErr, timeOut );
            if ( status != 0 )
            {
                throw new MojoExecutionException( "Failed to run I18NSync : returned " + status );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to run I18NSync", e );
        }
    }

    /**
     * @return the project classloader
     * @throws DependencyResolutionRequiredException failed to resolve project dependencies
     * @throws MalformedURLException configuration issue ?
     */
    protected ClassLoader getProjectClassLoader()
        throws DependencyResolutionRequiredException, MalformedURLException
    {
        getLog().debug( "AbstractMojo#getProjectClassLoader()" );

        List<?> compile = project.getCompileClasspathElements();
        URL[] urls = new URL[compile.size()];
        int i = 0;
        for ( Object object : compile )
        {
            if ( object instanceof Artifact )
            {
                urls[i] = ( (Artifact) object ).getFile().toURI().toURL();
            }
            else
            {
                urls[i] = new File( (String) object ).toURI().toURL();
            }
            i++;
        }
        return new URLClassLoader( urls, ClassLoader.getSystemClassLoader() );
    }

    /**
     * @param path file to add to the project compile directories
     */
    protected void addCompileSourceRoot( File path )
    {
        project.addCompileSourceRoot( path.getAbsolutePath() );
    }

    /**
     * Add project classpath element to a classpath URL set
     *
     * @param originalUrls the initial URL set
     * @return full classpath URL set
     * @throws MojoExecutionException some error occured
     */
    protected URL[] addProjectClasspathElements( URL[] originalUrls )
        throws MojoExecutionException
    {
        Collection<?> sources = project.getCompileSourceRoots();
        Collection<?> resources = project.getResources();
        Collection<?> dependencies = project.getArtifacts();
        URL[] urls = new URL[originalUrls.length + sources.size() + resources.size() + dependencies.size() + 2];

        int i = originalUrls.length;
        getLog().debug( "add compile source roots to GWTCompiler classpath " + sources.size() );
        i = addClasspathElements( sources, urls, i );
        getLog().debug( "add resources to GWTCompiler classpath " + resources.size() );
        i = addClasspathElements( resources, urls, i );
        getLog().debug( "add project dependencies to GWTCompiler  classpath " + dependencies.size() );
        i = addClasspathElements( dependencies, urls, i );
        try
        {
            urls[i++] = generateDirectory.toURL();
            urls[i] = new File( project.getBuild().getOutputDirectory() ).toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to convert project.build.outputDirectory to URL", e );
        }
        return urls;
    }

    /**
     * Need this to run both pre- and post- PLX-220 fix.
     *
     * @return a ClassLoader including plugin dependencies and project source foler
     * @throws MojoExecutionException failed to configure ClassLoader
     */
    protected ClassLoader getClassLoader( GwtRuntime runtime )
        throws MojoExecutionException
    {
        try
        {
            Collection<File> classpath = getClasspath( Artifact.SCOPE_COMPILE, runtime );
            URL[] urls = new URL[classpath.size()];
            int i = 0;
            for ( File file : classpath )
            {
                urls[i++] = file.toURL();
            }
            ClassLoader parent = getClass().getClassLoader();
            return new URLClassLoader( urls, parent.getParent() );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve project dependencies" );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Unexpecetd internal error" );
        }
    }

}
