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

import static org.codehaus.mojo.gwt.shell.scripting.ClasspathStrategy.CLASSPATH_VARIABLE;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which run a GWT module in the GWT Shell.
 *
 * @goal run
 * @execute phase=compile
 * @requiresDependencyResolution compile
 * @description Runs the the project in the GWTShell for development.
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
     * @parameter default-value="${basedir}/src/test/hosted"
     */
    // Parameter shared with EclipseMojo
    private File hostedWebapp;

    /**
     * URL that should be automatically opened in the GWT shell. For example com.myapp.gwt.Module/Module.html
     *
     * @parameter expression="${runTarget}"
     * @required
     */
    private String runTarget;

    /**
     * Runs the embedded GWT Tomcat server on the specified port.
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
     * Source Tomcat context.xml for GWT shell - copied to /gwt/localhost/ROOT.xml (used as the context.xml for the
     * SHELL - requires Tomcat 5.0.x format - hence no default).
     *
     * @parameter
     */
    private File contextXml;

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
        if (isNoServer())
        {
            String[] modules = getModules();
            if (modules.length != 1)
            {
                getLog().error(
                    "Running in 'noserver' mode you must specify the single module to run using -Dgwt.module=..." );
                throw new MojoExecutionException( "No single module specified" );
            }
            return modules[0];
        }
        int dash = runTarget.indexOf( '/' );
        return runTarget.substring( 0, dash );
    }

    /**
     * @return the startup URL to open in hosted browser (gwt 1.6+)
     */
    public String getStartupUrl()
       throws MojoExecutionException
    {
        if ( isNoServer() )
        {
            return runTarget;
        }
        int dash = runTarget.indexOf( '/' );
        String module = getRunModule();
        String renameTo = readModule( module ).getRenameTo();
        String modulePath = ( renameTo != null ? renameTo : module );
        return modulePath + '/' + runTarget.substring( dash + 1 );
    }

    protected String getFileName()
    {
        return "run";
    }

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            this.makeCatalinaBase();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build catalina.base", e );
        }
        if ( !this.getOutput().exists() )
        {
            this.getOutput().mkdirs();
        }

        // build it for the correct platform
        ScriptWriter script = scriptWriterFactory.getScript();
        script.createScript( this, getFileName() );
        String clazz = runtime.getVersion().getShellFQCN();
        switch ( runtime.getVersion() )
        {
            case ONE_DOT_FOUR:
            case ONE_DOT_FIVE:
                script.addVariable( "catalina.base", "\"" + getTomcat().getAbsolutePath() + "\"" );
                break;
        }
        script.executeClass( this, runtime, CLASSPATH_VARIABLE, clazz );

        script.print( " -gen \"" );
        script.print( getGen().getAbsolutePath() );
        script.print( "\" -logLevel " );
        script.print( getLogLevel() );
        script.print( " -style " );
        script.print( getStyle() );
        script.print( " -port " );
        script.print( Integer.toString( getPort() ) );
        if ( isNoServer() )
        {
            script.print( " -noserver " );
        }

        switch ( runtime.getVersion() )
        {
            case ONE_DOT_FOUR:
            case ONE_DOT_FIVE:
                script.print( " -out " );
                script.print( "\"" + hostedWebapp.getAbsolutePath() + "\"" );
                script.print( " " + getRunTarget() );
                break;
            default:
                script.print( " -war " );
                script.print( "\"" + hostedWebapp.getAbsolutePath() + "\"" );
                script.print( " -startupUrl " );
                script.print( "\"" + getStartupUrl() + "\"" );
                script.print( " " + getRunModule() );
                break;
        }
        script.println();

        // run it
        runScript( script.getExecutable() );
    }

    /**
     * Create embedded GWT tomcat base dir based on properties.
     *
     * @throws Exception
     */
    public void makeCatalinaBase()
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
        new MakeCatalinaBase( this.getTomcat(), this.getWebXml(), this.getShellServletMappingURL() ).setup();

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
}
