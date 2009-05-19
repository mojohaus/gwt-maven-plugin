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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.shell.ArtifactNameUtil;
import org.codehaus.mojo.gwt.shell.ClasspathBuilder;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * Abstract Support class for all GWT-related operations. Provide GWT runtime resolution based on plugin configuration
 * and/or project dependencies. Creates the runtime dependencies list to be used by GWT tools.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public abstract class AbstractGwtMojo
    extends AbstractMojo
    implements Contextualizable
{
    /** GWT artifacts groupId */
    public static final String GWT_GROUP_ID = "com.google.gwt";

    // --- Some Maven tools ----------------------------------------------------

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     */
    protected ArchiverManager archiverManager;

    /**
     * @component
     */
    protected ClasspathBuilder classpathBuilder;

    // --- Some MavenSession related structures --------------------------------

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * The maven project descriptor
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.artifacts}"
     * @readolny
     * @required
     */
    private Set<Artifact> artifacts;

    // --- Plugin parameters ---------------------------------------------------

    /**
     * GWT version used to build dependency paths, should match the "version" in the Maven repo. If not set, will be
     * autodetected from project com.google.gwt:gwt-user dependency
     *
     * @parameter
     */
    private String gwtVersion;

    /**
     * Location on filesystem where GWT is installed - for manual mode (existing GWT on machine). Setting this parameter
     * will disable gwtVersion.
     *
     * @parameter expression="${google.webtoolkit.home}"
     */
    private File gwtHome;

    /**
     * Folder where generated-source will be created (automatically added to compile classpath).
     *
     * @parameter default-value="${project.build.directory}/generated-sources/gwt"
     * @required
     */
    private File generateDirectory;

    //------------------------------
    // Plexus Lifecycle
    //------------------------------
    public final void contextualize( Context context )
        throws ContextException
    {
        PlexusContainer plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
        try
        {
            archiverManager = (ArchiverManager) plexusContainer.lookup( ArchiverManager.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new ContextException( e.getMessage(), e );
        }
    }

    /**
     * Add classpath elements to a classpath URL set
     *
     * @param elements the initial URL set
     * @param urls the urls to add
     * @param startPosition the position to insert URLS
     * @return full classpath URL set
     * @throws MojoExecutionException some error occured
     */
    protected int addClasspathElements( Collection<?> elements, URL[] urls, int startPosition )
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
                throw new MojoExecutionException(
                                                  "Failed to convert original classpath element " + object + " to URL.",
                                                  e );
            }
            startPosition++;
        }
        return startPosition;
    }

    /**
     * Build the GWT classpath for the specified scope
     *
     * @param scope Artifact.SCOPE_COMPILE or Artifact.SCOPE_TEST
     * @param runtime the GwtRuntime used by this plugin execution
     * @return a collection of dependencies as Files for the specified scope.
     */
    public Collection<File> getClasspath( String scope, GwtRuntime runtime )
        throws MojoExecutionException, DependencyResolutionRequiredException
    {
        return classpathBuilder.buildClasspathList( getProject(), scope, runtime, true, true, artifacts );
    }

    /**
     * Build a GwtRuntime based on plugin configuration or the mavenProject dependencies.
     *
     * @return The GWT Runtime
     * @throws MojoExecutionException some error occured
     */
    public GwtRuntime getGwtRuntime()
        throws MojoExecutionException
    {
        if ( gwtHome != null )
        {
            getLog().info( "using GWT jars from local installation " + gwtHome );
            if ( !gwtHome.exists() )
            {
                throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
            }
            File userJar = new File( gwtHome, "gwt-user.jar" );
            if ( !userJar.exists() )
            {
                throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
            }
            File devJar = new File( gwtHome, ArtifactNameUtil.guessDevJarName() );
            if ( !devJar.exists() )
            {
                throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
            }
            return new GwtRuntime( userJar, devJar );
        }

        if ( gwtVersion != null )
        {
            getLog().info( "using GWT jars for specified version " + gwtVersion );
            return getGwtRuntimeForVersion( gwtVersion );
        }

        // Autodetect
        detectGwtVersion();
        if ( gwtVersion == null )
        {
            getLog().error( "no gwtHome, gwtVersion or com.google.gwt:gwt-user dependency set" );
            throw new MojoExecutionException( "Cannot resolve GWT version" );
        }

        checkGwtDevAsDependency();

        return getGwtRuntimeForVersion( gwtVersion );
    }

    /**
     * Detect the GWT version to use
     */
    private void detectGwtVersion()
    {
        Collection<Artifact> artifacts = getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            if ( AbstractGwtMojo.GWT_GROUP_ID.equals( artifact.getGroupId() )
                && "gwt-user".equals( artifact.getArtifactId() ) )
            {
                gwtVersion = artifact.getVersion();
                if ( gwtVersion != null )
                {
                    getLog().info( "using GWT jars from project dependencies : " + gwtVersion );
                }
                break;
            }
        }
        if ( gwtVersion == null )
        {
            if ( getProject().getDependencyManagement() != null && getProject().getDependencyManagement().getDependencies() != null )
            {
                Collection<Dependency> dependencyManagement = getProject().getDependencyManagement().getDependencies();
                for ( Dependency dependency : dependencyManagement )
                {
                    if ( AbstractGwtMojo.GWT_GROUP_ID.equals( dependency.getGroupId() )
                        && "gwt-user".equals( dependency.getArtifactId() ) )
                    {
                        gwtVersion = dependency.getVersion();
                        getLog().info( "using GWT jars from project dependencyManagement section : " + gwtVersion );
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check that gwt-dev is not define in dependencies : this can produce version conflicts with other dependencies, as
     * gwt-dev is a "uber-jar" with some commons-* and jetty libs inside.
     */
    private void checkGwtDevAsDependency()
    {
        for ( Iterator iterator = getProject().getArtifacts().iterator(); iterator.hasNext(); )
        {
            Artifact artifact = (Artifact) iterator.next();
            if ( AbstractGwtMojo.GWT_GROUP_ID.equals( artifact.getGroupId() )
                && "gwt-dev".equals( artifact.getArtifactId() ) )
            {
                getLog().warn( "You should not declare gwt-dev as a project dependency. This may introduce complex dependency conflicts" );
            }
        }
    }

    /**
     * @param version The GWT version to retrieve from repository
     * @return The GWT Runtime
     * @throws MojoExecutionException some error occured
     */
    private GwtRuntime getGwtRuntimeForVersion( String version )
        throws MojoExecutionException
    {
        Artifact gwtUser =
            artifactFactory.createArtifactWithClassifier( GWT_GROUP_ID, "gwt-user", version, "jar", null );
        Artifact gwtDev =
            artifactFactory.createArtifactWithClassifier( GWT_GROUP_ID, "gwt-dev", version, "jar",
                                                          ArtifactNameUtil.getPlatformName() );
        Artifact gwtNatives =
            artifactFactory.createArtifactWithClassifier( GWT_GROUP_ID, "gwt-dev", version, "zip",
                                                          ArtifactNameUtil.getPlatformName() + "-libs" );

        try
        {
            resolver.resolve( gwtUser, remoteRepositories, localRepository );
            resolver.resolve( gwtDev, remoteRepositories, localRepository );
            resolver.resolve( gwtNatives, remoteRepositories, localRepository );
            unpackNativeLibraries( gwtNatives.getFile() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "artifact not found - " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "artifact resolver problem - " + e.getMessage(), e );
        }
        return new GwtRuntime( gwtUser.getFile(), gwtDev.getFile(), version );
    }

    /**
     * Unpack the GWT native libraries in the repository so that Hosted mode browser can be executed without requirement
     * to install GWT on computer.
     *
     * @throws MojoExecutionException some error occured
     */
    private void unpackNativeLibraries( File zip )
        throws MojoExecutionException
    {
        try
        {
            UnArchiver unArchiver = getArchiverManager().getUnArchiver( zip );
            unArchiver.setSourceFile( zip );
            unArchiver.setDestDirectory( zip.getParentFile() );
            unArchiver.extract();
            unArchiver.setOverwrite( false );
            getLog().info( "Unpack native libraries required to run GWT" );
        }
        catch ( Exception e )
        {
            getLog().error( "Failed to unpack native libraries required to run hosted browser", e );
            throw new MojoExecutionException( "GWT setup failed" );
        }
    }

    /**
     * @param path file to add to the project compile directories
     */
    protected void addCompileSourceRoot( File path )
    {
        getProject().addCompileSourceRoot( path.getAbsolutePath() );
    }

    /**
     * @return the project
     */
    public MavenProject getProject()
    {
        return project;
    }


    public ArtifactRepository getLocalRepository()
    {
        return this.localRepository;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return this.remoteRepositories;
    }

    public File getGenerateDirectory()
    {
        if (!generateDirectory.exists())
        {
            getLog().debug( "Creating target directory " + generateDirectory.getAbsolutePath() );
            generateDirectory.mkdirs();
        }
        return generateDirectory;
    }

    protected ArchiverManager getArchiverManager()
    {
        return archiverManager;
    }

    protected void setProject( MavenProject project )
    {
        this.project = project;
    }

    public Set<Artifact> getProjectArtifacts()
    {
        return artifacts;
    }
}
