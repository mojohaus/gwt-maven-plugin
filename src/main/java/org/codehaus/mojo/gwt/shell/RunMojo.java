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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.shell.scripting.RunScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptUtil;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which run a GWT module in the GWT Shell.
 * 
 * @goal gwt
 * @execute phase=compile
 * @requiresDependencyResolution compile
 * @description Runs the the project in the GWTShell for development.
 * @author ccollins
 * @author cooper
 */
public class RunMojo
    extends AbstractGwtShellMojo
    implements RunScriptConfiguration
{

    /**
     * URL that should be automatically opened by default in the GWT shell.
     * 
     * @parameter
     */
    private String runTarget;

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.GwtShellScriptConfiguration#getRunTarget()
     */
    public String getRunTarget()
    {
        return this.runTarget;
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        initialize();
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
        ScriptWriter writer = scriptWriterFactory.getScriptWriter();
        File exec = writer.writeRunScript( this );

        // run it
        ScriptUtil.runScript( exec );
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
}
