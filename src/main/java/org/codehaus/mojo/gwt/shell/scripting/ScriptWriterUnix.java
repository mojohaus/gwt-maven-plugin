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
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.PlatformUtil;

/**
 * Handler for writing shell scripts for the mac and linux platforms.
 *
 * @author ccollins
 * @author rcooper
 * @plexus.component role="org.codehaus.mojo.gwt.shell.scripting.ScriptWriter" role-hint="unix"
 */
public class ScriptWriterUnix
    extends AbstractScriptWriter
{
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
     * @param runtime TODO
     * @return
     * @throws MojoExecutionException
     */
    protected PrintWriter createScript( final GwtShellScriptConfiguration mojo, File file,
                                                     final String scope, GwtRuntime runtime )
        throws MojoExecutionException
    {

        PrintWriter writer = null;
        try
        {
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

        try
        {
            Collection<File> classpath =
                buildClasspathUtil.buildClasspathList( mojo.getProject(), scope, runtime, mojo.getSourcesOnPath(),
                                                       mojo.getResourcesOnPath() );
            writer.print( "export CLASSPATH=" );
            Iterator it = classpath.iterator();
            while ( it.hasNext() )
            {
                File f = (File) it.next();
                if ( it.hasNext() )
                    writer.print( "\"" + f.getAbsolutePath() + "\":" );
                else
                    writer.print( "\"" + f.getAbsolutePath() + "\"" );
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error creating script - " + file, e );
        }
        writer.println();
        writer.println();
        return writer;
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
            ProcessWatcher pw = new ProcessWatcher( "chmod +x " + file.getAbsolutePath() );
            pw.startProcess( System.out, System.err );
            pw.waitFor();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * {@inheritDoc}
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

}
