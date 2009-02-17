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

/**
 * Handler for writing cmd scripts for the windows platform.
 *
 * @author ccollins
 * @author rcooper
 * @version $Id$
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
     * @param file
     * @param config
     *
     * @return
     * @throws MojoExecutionException
     */
    protected PrintWriter createScript( final GwtShellScriptConfiguration config, File file )
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

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.AbstractScriptWriter#getPlatformClasspathVariableDefinition()
     */
    protected String getPlatformClasspathVariableDefinition()
    {
        return "set CLASSPATH=";
    }

}
