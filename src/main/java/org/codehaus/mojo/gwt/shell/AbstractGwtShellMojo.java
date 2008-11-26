package org.codehaus.mojo.gwt.shell;

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
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.AbstractGwtMojo;
import org.codehaus.mojo.gwt.shell.scripting.GwtShellScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriterFactory;

/**
 * Abstract Mojo for GWT-Maven.
 *
 * @author ccollins
 * @author cooper
 * @author willpugh
 */
public abstract class AbstractGwtShellMojo
    extends AbstractGwtModuleMojo
    implements GwtShellScriptConfiguration
{

    /**
     * @component
     */
    protected ScriptWriterFactory scriptWriterFactory;

    /**
     * @component
     */
    protected ClasspathBuilder buildClasspathUtil;


    // GWT-Maven properties

    /**
     * Location on filesystem where project should be built.
     *
     * @parameter expression="${project.build.directory}"
     */
    private File buildDir;

    /**
     * Set the GWT version number - used to build dependency paths, should match the "version" in the Maven repo.
     *
     * @parameter
     */
    private String gwtVersion;

    /**
     * Location on filesystem where GWT is installed - for manual mode (existing GWT on machine - not needed for
     * automatic mode).
     *
     * @parameter expression="${google.webtoolkit.home}"
     */
    private File gwtHome;

    /**
     * Location on filesystem where GWT will write output files (-out option to GWTCompiler).
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File output;

    /**
     * Location on filesystem where GWT will write generated content for review (-gen option to GWTCompiler).
     *
     * @parameter expression="${project.build.directory}/.generated"
     */
    private File gen;

    /**
     * GWT logging level (-logLevel ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL).
     *
     * @parameter default-value="INFO"
     */
    private String logLevel;

    /**
     * GWT JavaScript compiler output style (-style OBF[USCATED], PRETTY, or DETAILED).
     *
     * @parameter default-value="OBF"
     */
    private String style;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     *
     * @parameter default-value="false"
     */
    private boolean noServer;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * 
     * @parameter expression="${google.webtoolkit.extrajvmargs}"
     */
    private String extraJvmArgs;

    /**
     * Whether or not to add compile source root to classpath.
     *
     * @parameter default-value="true"
     */
    private boolean sourcesOnPath;

    /**
     * Whether or not to add resources root to classpath.
     *
     * @parameter default-value="true"
     */
    private boolean resourcesOnPath;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

    /**
     * Specifies the mapping URL to be used with the shell servlet.
     *
     * @parameter default-value="/*"
     */
    private String shellServletMappingURL;

    // methods

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        initialize();
        doExecute();
    }

    protected abstract void doExecute()
        throws MojoExecutionException, MojoFailureException;

    /**
     * {@inheritDoc}
     */
    public void initialize()
        throws MojoExecutionException
    {
        if ( gwtVersion == null )
        {
            for ( Iterator iterator = project.getArtifacts().iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();
                if ( AbstractGwtMojo.GWT_GROUP_ID.equals( artifact.getGroupId() )
                    && "gwt-user".equals( artifact.getArtifactId() ) )
                {
                    gwtVersion = artifact.getVersion();
                    getLog().info( "GWT version found in project dependencies : " + gwtVersion );
                    break;
                }
            }
        }
        if ( gwtVersion == null )
        {
            getLog().error( "gwtVersion not set and no com.google.gwt:gwt-user dependency found in project" );
            throw new MojoExecutionException( "cannot resolve GWT version" );
        }
    }

    /**
     * Helper hack for classpath problems, used as a fallback.
     *
     * @return
     */
    protected ClassLoader fixThreadClasspath()
    {
        try
        {
            ClassWorld world = new ClassWorld();

            // use the existing ContextClassLoader in a realm of the classloading space
            ClassRealm root = world.newRealm( "gwt-plugin", Thread.currentThread().getContextClassLoader() );
            ClassRealm realm = root.createChildRealm( "gwt-project" );

            for ( Iterator it = buildClasspathUtil.buildClasspathList( this, Artifact.SCOPE_COMPILE ).iterator(); it.hasNext(); )
            {
                realm.addConstituent( ( (File) it.next() ).toURI().toURL() );
            }

            Thread.currentThread().setContextClassLoader( realm.getClassLoader() );
            // /System.out.println("AbstractGwtMojo realm classloader = " + realm.getClassLoader().toString());

            return realm.getClassLoader();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

    //
    // accessors/mutators
    //

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getBuildDir()
     */
    public File getBuildDir()
    {
        return this.buildDir;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getCompileTarget()
     */
    public String[] getCompileTarget()
    {
        return getModules();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getExtraJvmArgs()
     */
    public String getExtraJvmArgs()
    {
        return this.extraJvmArgs;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGen()
     */
    public File getGen()
    {
        return this.gen;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGwtHome()
     */
    public File getGwtHome()
    {
        return this.gwtHome;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getLogLevel()
     */
    public String getLogLevel()
    {
        return this.logLevel;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isNoServer()
     */
    public boolean isNoServer()
    {
        return this.noServer;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getOutput()
     */
    public File getOutput()
    {
        return this.output;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getStyle()
     */
    public String getStyle()
    {
        return this.style;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getShellServletMappingURL()
     */
    public String getShellServletMappingURL()
    {
        return this.shellServletMappingURL;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGwtVersion()
     */
    public String getGwtVersion()
    {
        return this.gwtVersion;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getSourcesOnPath()
     */
    public boolean getSourcesOnPath()
    {
        return this.sourcesOnPath;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getResourcesOnPath()
     */
    public boolean getResourcesOnPath()
    {
        return resourcesOnPath;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isEnableAssertions()
     */
    public boolean isEnableAssertions()
    {
        return this.enableAssertions;
    }


}
