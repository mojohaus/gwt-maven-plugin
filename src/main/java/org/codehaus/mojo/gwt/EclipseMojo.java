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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Goal which install GWT artifacts in local repository.
 *
 * @goal eclipse
 * @phase validate
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class EclipseMojo
    extends AbstractGwtMojo
{
    /**
     * The GWT module to setup.
     *
     * @parameter
     * @required
     */
    private String module;

    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File outputDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    protected ArchiverManager archiverManager;


    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        unpackNativeLibraries();

        createLaunchConfigurationForHostedModeBrowser();
    }

    private void unpackNativeLibraries()
        throws MojoFailureException
    {
        // How to access the plugin MavenProject -> getCompileClasspathElements()
        URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
        URL[] urls = cl.getURLs();
        for ( int i = 0; i < urls.length; i++ )
        {
            if ( urls[i].getFile().endsWith( ".zip" ) )
            {
                File file = new File( urls[i].getFile() );
                try
                {
                    UnArchiver unArchiver = archiverManager.getUnArchiver( file );
                    unArchiver.setSourceFile( file );
                    unArchiver.setDestDirectory( file.getParentFile() );
                    unArchiver.extract();
                    unArchiver.setOverwrite( false );
                    getLog().info( "Unpack native libraries required to run hosted browser" );
                }
                catch (Exception e)
                {
                    getLog().error( "Failed to unpack native libraries required to run hosted browser" );
                }
                break;
            }
        }
    }

    private void createLaunchConfigurationForHostedModeBrowser()
        throws MojoExecutionException
    {

        File launchFile = new File( getProject().getBasedir(), module + ".launch" );
        if (launchFile.exists())
        {
            return;
        }

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading( EclipseMojo.class, "" );

        Map context = new HashMap();
        context.put( "src", getProject().getBuild().getSourceDirectory() );
        context.put( "module", module );
        int idx = module.lastIndexOf( '.' );
        context.put( "page", module.substring( idx + 1 ) );
        int basedir = getProject().getBasedir().getAbsolutePath().length();
        context.put( "out", outputDirectory.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "project", getProject().getArtifactId() );
        // Retrieve GWT 
        File gwtDevJarPath = null;
        URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
        URL[] urls = cl.getURLs();
        for ( int i = 0; i < urls.length; i++ )
        {
            if ( urls[i].getFile().indexOf( "gwt-dev" ) >= 0 && urls[i].getFile().endsWith( ".jar" ) )
            {
                gwtDevJarPath = new File( urls[i].getFile() );
                break;
            }
        }
        if ( gwtDevJarPath == null )
        {
            getLog().error( "Failed to retrieve the path of gwt-dev-XX.jar" );
        }
        else
        {
            getLog().info( "gwt-dev-XX.jar found at " + gwtDevJarPath.getAbsolutePath() );
        }
        context.put( "gwtDevJarPath", gwtDevJarPath.getAbsolutePath() );

        try
        {
            Writer configWriter = new FileWriter( launchFile );
            Template template = cfg.getTemplate( "launch.fm" );
            template.process( context, configWriter );
            configWriter.flush();
            configWriter.close();
            getLog().info( "Write launch configuration for GWT module : " + launchFile.getAbsolutePath() );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to write launch configuration", ioe );
        }
        catch ( TemplateException te )
        {
            throw new MojoExecutionException( "Unable to merge freemarker template", te );
        }
    }
}
