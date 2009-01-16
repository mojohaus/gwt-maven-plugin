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
import org.codehaus.mojo.gwt.GwtRuntime;

/**
 * Handler for writing cmd scripts for the windows platform.
 *
 * @author ccollins
 * @author rcooper
 * @plexus.component role="org.codehaus.mojo.gwt.shell.scripting.ScriptWriter" role-hint="windows"
 */
public class ScriptWriterWindows
    extends AbstractScriptWriter
{
    @Override
    protected String getScriptExtension()
    {
        return ".cmd";
    }

    /**
     * Util to get a PrintWriter with Windows preamble.
     *
     * @param config
     * @param file
     * @param runtime TODO
     * @return
     * @throws MojoExecutionException
     */
    protected PrintWriter createScript( final GwtShellScriptConfiguration config, File file,
                                                       final String scope, GwtRuntime runtime )
        throws MojoExecutionException
    {

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new FileWriter( file ) );
            writer.println( "@echo off" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating script - " + file, e );
        }

        /*
        try
        {
            Collection<File> classpath =
                buildClasspathUtil.buildClasspathList( config.getProject(), scope, runtime, config.getSourcesOnPath(),
                                                       config.getResourcesOnPath() );
            writer.print( "set CLASSPATH=" );

            StringBuffer cpString = new StringBuffer();

            for ( File f : classpath )
            {
                cpString.append( "\"" + f.getAbsolutePath() + "\";" );
                // break the line at 4000 characters to try to avoid max size
                if ( cpString.length() > 4000 )
                {
                    writer.println( cpString );
                    cpString = new StringBuffer();
                    writer.print( "set CLASSPATH=%CLASSPATH%;" );
                }
            }
            writer.println( cpString );
            writer.println();
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error creating script - " + file, e );
        }
        */
        writer.println();
        return writer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.AbstractScriptWriter#getExtraJvmArgs(org.codehaus.mojo.gwt.shell.scripting.RunScriptConfiguration)
     */
    protected String getExtraJvmArgs( GwtShellScriptConfiguration configuration )
    {
        String extra = ( configuration.getExtraJvmArgs() != null ) ? configuration.getExtraJvmArgs() : "";
        return extra;
    }

    protected String getPlatformClasspathVariable()
    {
        return "%CLASSPATH%";
    }

}
