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
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which run a GWT module in the GWT Hosted mode.
 * 
 * @goal run
 * @execute phase=compile
 * @requiresDependencyResolution test
 * @description Runs the the project in the GWT Hosted mode for development.
 * @author ccollins
 * @author cooper
 * @version $Id$
 */
public class RunMojo
    extends AbstractGwtWebMojo
{
    /**
     * Location of the hosted-mode web application structure.
     *
     * @parameter default-value="${basedir}/war"
     */
    // Parameter shared with EclipseMojo
    private File hostedWebapp;

    /**
     * The MavenProject executed by the "compile" phase
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * URL that should be automatically opened in the GWT shell. For example com.myapp.gwt.Module/Module.html.
     * <p>
     * When the host page is outside the module "public" folder (for example, at webapp root), the module MUST be
     * specified (using a single &lt;module&gt; in configuration or by setting <code>-Dgwt.module=..</code>) and the
     * runTarget parameter can only contain the host page URI.
     * <p>
     * When the GWT module host page is part of the module "public" folder, the runTarget MAY define the full GWT module
     * path (<code>com.myapp.gwt.Module/Module.html</code>) that will be automatically converted according to the
     * <code>rename-to</code> directive into <code>renamed/Module.html</code>.
     *
     * @parameter expression="${runTarget}"
     * @required
     */
    private String runTarget;

    /**
     * Forked process execution timeOut (in seconds). Primary used for integration-testing.
     * @parameter
     */
    @SuppressWarnings("unused")
    private int runTimeOut;

    /**
     * Runs the embedded GWT server on the specified port.
     *
     * @parameter default-value="8888"
     */
    private int port;

    /**
     * Specify the location on the filesystem for the generated embedded Tomcat directory.
     *
     * @parameter default-value="${project.build.directory}/tomcat"
     */
    private File tomcat;

    /**
     * Location of the compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readOnly
     */
    private File buildOutputDirectory;


    /**
     * Source Tomcat context.xml for GWT shell - copied to /gwt/localhost/ROOT.xml (used as the context.xml for the
     * SHELL - requires Tomcat 5.0.x format - hence no default).
     *
     * @parameter
     */
    private File contextXml;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     * <p>
     * Can be set from command line using '-Dgwt.noserver=...'
     * 
     * @parameter default-value="false" expression="${gwt.noserver}"
     */
    private boolean noServer;

    /**
     * Specifies the mapping URL to be used with the shell servlet.
     * 
     * @parameter default-value="/*"
     */
    private String shellServletMappingURL;

    public String getRunTarget()
    {
        return this.runTarget;
    }

    /**
     * @return the GWT module to run (gwt 1.6+)
     */
    public String getRunModule()
    throws MojoExecutionException
    {
        String[] modules = getModules();
        if ( noServer )
        {
            if (modules.length != 1)
            {
                getLog().error(
                    "Running in 'noserver' mode you must specify the single module to run using -Dgwt.module=..." );
                throw new MojoExecutionException( "No single module specified" );
            }
            return modules[0];
        }
        if ( modules.length == 1 )
        {
            // A single module is set, no ambiguity
            return modules[0];
        }
        int dash = runTarget.indexOf( '/' );
        if ( dash > 0 )
        {
            return runTarget.substring( 0, dash );
        }
        // The runTarget MUST start with the full GWT module path
        throw new MojoExecutionException( "You MUST specify the GWT module to run using -Dgwt.module" );
    }

    /**
     * @return the startup URL to open in hosted browser (gwt 1.6+)
     */
    public String getStartupUrl()
       throws MojoExecutionException
    {
        if ( noServer )
        {
            return runTarget;
        }
        int dash = runTarget.indexOf( '/' );
        String module = getRunModule();
        if ( dash > 0 )
        {
            String prefix = runTarget.substring( 0, dash );
            if ( prefix.equals( module ) )
            {
                // runTarget includes the GWT module full path. Lets apply the rename-to directive
                String renameTo = readModule( module ).getRenameTo();
                String modulePath = ( renameTo != null ? renameTo : module );
                return modulePath + '/' + runTarget.substring( dash + 1 );
            }
        }
        return runTarget;
    }

    protected String getFileName()
    {
        return "run";
    }

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {
        String clazz = runtime.getVersion().getShellFQCN();
        JavaCommand cmd = new JavaCommand( clazz, runtime )
            .withinScope( Artifact.SCOPE_RUNTIME )
            .arg( runtime.getVersion().getWebOutputArgument() )
            .arg( quote( hostedWebapp.getAbsolutePath() ) )
            .arg( "-gen" )
            .arg( quote( getGen().getAbsolutePath() ) )
            .arg( "-logLevel" )
            .arg( getLogLevel() )
            .arg( "-style" )
            .arg( getStyle() )
            .arg( "-port" )
            .arg( Integer.toString( getPort() ) )
            .arg( noServer,
                "-noserver" );

        switch ( runtime.getVersion() )
        {
            case ONE_DOT_FOUR:
            case ONE_DOT_FIVE:
                try
                {
                    this.makeCatalinaBase();
                }
                catch ( Exception e )
                {
                    throw new MojoExecutionException( "Unable to build catalina.base", e );
                }
                cmd.systemProperty( "catalina.base", quote( getTomcat().getAbsolutePath() ) )
                    .arg( getRunTarget() );
                break;
            default:
                setupExplodedWar();
                cmd.arg( "-startupUrl" )
                    .arg( quote( getStartupUrl() ) )
                    .arg( getRunModule() );
                break;
        }

        cmd.execute();
    }

    private void setupExplodedWar()
    throws MojoExecutionException
    {
        getLog().info( "create exploded Jetty webapp in " + hostedWebapp );

        File classes = new File( hostedWebapp, "WEB-INF/classes" );
        classes.mkdirs();

        if ( !buildOutputDirectory.getAbsolutePath().equals( classes.getAbsolutePath() ) )
        {
            getLog().warn( "Your POM <build><outputdirectory> does not match your "
                                + "hosted webapp WEB-INF/classes folder for GWT Hosted browser to see your classes." );
            try
            {
                FileUtils.copyDirectory( buildOutputDirectory, classes );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy classes to " + classes , e );
            }
        }


        File lib = new File( hostedWebapp, "WEB-INF/lib" );
        lib.mkdirs();

        Collection<Artifact> artifacts = getProjectArtifacts();
        for ( Artifact artifact : artifacts )
        {
            try
            {
                // Using m2eclipse with "resolve workspace dependencies" the artifact is the buildOutputDirectory
                if ( ! artifact.getFile().isDirectory() )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), lib );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy runtime dependency " + artifact, e );
            }
        }

    }

    /**
     * Create embedded GWT tomcat base dir based on properties.
     *
     * @throws Exception
     */
    private void makeCatalinaBase()
        throws Exception
    {
        getLog().debug( "make catalina base for embedded Tomcat" );

        if ( this.getWebXml() != null && this.getWebXml().exists() )
        {
            this.getLog().info( "source web.xml present - " + this.getWebXml() + " - using it with embedded Tomcat" );
        }
        else
        {
            this.getLog().info( "source web.xml NOT present, using default empty web.xml for shell" );
        }

        // note that MakeCatalinaBase will use emptyWeb.xml if webXml does not exist
        new MakeCatalinaBase( this.getTomcat(), this.getWebXml(), shellServletMappingURL ).setup();

        if ( ( this.getContextXml() != null ) && this.getContextXml().exists() )
        {
            this.getLog().info(
                                "contextXml parameter present - " + this.getContextXml()
                                    + " - using it for embedded Tomcat ROOT.xml" );
            FileUtils.copyFile( this.getContextXml(), new File( this.getTomcat(), "conf/gwt/localhost/ROOT.xml" ) );
        }
    }


    public File getContextXml()
    {
        return this.contextXml;
    }

    public int getPort()
    {
        return this.port;
    }

    public File getTomcat()
    {
        return this.tomcat;
    }

    /**
     * @param runTimeOut the runTimeOut to set
     */
    public void setRunTimeOut( int runTimeOut )
    {
        setTimeOut( runTimeOut );
    }

    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }
}
