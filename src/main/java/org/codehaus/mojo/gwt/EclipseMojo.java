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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

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
    extends AbstractGwtModuleMojo
{

    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File outputDirectory;

    /**
     * Additional parameters to append to the module URL. For example, gwt-log users will set "log_level=DEBUG"
     *
     * @parameter
     */
    private String additionalPageParameters;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    protected ArchiverManager archiverManager;

    /** path to the gwt-dev JAR */
    private File gwtDevJarPath;

    /** 
     * @param parameters additional parameter for module URL
     */      
    public void setAdditionalPageParameters( String parameters )
    {
        // escape the '&' char used for multiple parameters as the result must be XML compliant
        this.additionalPageParameters = StringUtils.replace( parameters, "&", "&amp;" );
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        unpackNativeLibraries();

        for ( String module : getModules() )
        {
            createLaunchConfigurationForHostedModeBrowser( module );
        }
    }

    /**
     * unpack the GWT runtime native dependencies in the local repository
     * @throws MojoFailureException some error occured
     */
    protected void unpackNativeLibraries()
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
                catch ( Exception e )
                {
                    getLog().error( "Failed to unpack native libraries required to run hosted browser" );
                }
                break;
            }
        }
    }

    /** 
     * create an Eclipse launch configuration file to Eclipse to run the module in hosted browser
     * @param module the GWT module
     * @throws MojoExecutionException some error occured
     */
    private void createLaunchConfigurationForHostedModeBrowser( String module )
        throws MojoExecutionException
    {

        File launchFile = new File( getProject().getBasedir(), module + ".launch" );
        if ( launchFile.exists() )
        {
            return;
        }

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading( EclipseMojo.class, "" );

        Map < String, Object > context = new HashMap < String, Object > ();
        List < String > sources = getProjectSourceDirectories();
        context.put( "sources", sources );
        context.put( "module", module );
        int idx = module.lastIndexOf( '.' );
        String page = module.substring( idx + 1 ) + ".html";
        if ( additionalPageParameters != null )
        {
            page += "?" + additionalPageParameters;
        }
        context.put( "page", page );
        int basedir = getProject().getBasedir().getAbsolutePath().length();
        context.put( "out", outputDirectory.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "project", getProjectName() );
        File path = getPlatformDependentGWTDevJar();
        context.put( "gwtDevJarPath", path.getAbsolutePath() );

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

    /**
     * getProject().getCompileSourceRoots(); is not adequate as we can't assume the mojo is
     * running as part of a phase-based build (user may execute 'mvn gwt:eclipse').
     * 
     * @return project source directories
     */
    protected List < String > getProjectSourceDirectories()
    {
        List < String > sources = new ArrayList < String > ();
        sources.add( getProject().getBuild().getSourceDirectory() );
        if ( generateDirectory.exists() )
        {
            sources.add( generateDirectory.getAbsolutePath() );
        }
        return sources;
    }

    /**
     * Read the Eclipse project name for .project file. Fall back to artifactId on error
     * @return project name in eclipse workspace
     */
    protected String getProjectName()
    {
        File dotProject = new File( getProject().getBasedir(), ".project" );
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( new FileReader( dotProject ) );
            return dom.getChild( "name" ).getValue();
        }
        catch ( Exception e )
        {
            getLog().warn( "Failed to read the .project file" );
            return getProject().getArtifactId();
        }
    }

    /**
     * @return gwt-dev JAR path
     */
    protected File getPlatformDependentGWTDevJar()
    {
        if ( gwtDevJarPath == null )
        {
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
        }
        return gwtDevJarPath;
    }
}
