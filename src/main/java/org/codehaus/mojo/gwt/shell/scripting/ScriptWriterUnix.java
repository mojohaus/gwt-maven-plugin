package org.codehaus.mojo.gwt.shell.scripting;

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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.shell.ClasspathBuilder;
import org.codehaus.mojo.gwt.shell.PlatformUtil;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Handler for writing shell scripts for the mac and linux platforms.
 *
 * @author ccollins
 * @author rcooper
 * @version $Id$
 * @plexus.component role="org.codehaus.mojo.gwt.shell.scripting.ScriptWriter" role-hint="unix"
 */
public class ScriptWriterUnix
    extends AbstractScriptWriter
{

    /**
     * @param buildClasspathUtil
     */
    public ScriptWriterUnix( ClasspathBuilder buildClasspathUtil )
    {
        super( buildClasspathUtil );
    }

    @Override
    protected String getExtraJvmArgs( GwtShellScriptConfiguration configuration )
    {
        String extra = ( configuration.getExtraJvmArgs() != null ) ? configuration.getExtraJvmArgs() : "";
        if ( PlatformUtil.OS_NAME.startsWith( "mac" ) && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
        {
            extra = "-XstartOnFirstThread " + extra;
        }
        return extra;
    }

    /**
     * Util to get a PrintWriter with Unix preamble and classpath.
     *
     * @param mojo
     * @param file
     * @throws MojoExecutionException
     */
    protected void createScript( final GwtShellScriptConfiguration mojo, File file )
        throws MojoExecutionException
    {
        try
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
            chmodUnixFile( file );
            writer = new PrintWriter( new FileWriter( file ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating script - " + file, e );
        }
        File sh = new File( "/bin/bash" );

        if ( !sh.exists() )
        {
            sh = new File( "/usr/bin/bash" );
        }

        if ( !sh.exists() )
        {
            sh = new File( "/bin/sh" );
        }
        writer.println( "#!" + sh.getAbsolutePath() );
        writer.println();
    }

    @Override
    protected String getPlatformClasspathVariableDefinition()
    {
        return "export CLASSPATH=";
    }

    /**
     * Util to chmod Unix file.
     *
     * @param file
     */
    private void chmodUnixFile( File file )
    {
        try
        {
            Commandline cmd = new Commandline( "chmod +x \"" + file.getAbsolutePath() + "\"" );
            StreamConsumer consumer = new CommandLineUtils.StringStreamConsumer();
            int status = CommandLineUtils.executeCommandLine( cmd, consumer, consumer );
            if ( status != 0 )
            {
                throw new IllegalStateException( "Failed to chmod script file as executable" );
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.AbstractScriptWriter#getScriptExtension()
     */
    protected String getScriptExtension()
    {
        return ".sh";
    }

    protected String getPlatformClasspathVariable()
    {
        return "$CLASSPATH";
    }

	@Override
	protected String getPlatformClasspathVariableReference()
    {
        return " -cp \"" + getPlatformClasspathVariable() + "\" ";
    }

}
