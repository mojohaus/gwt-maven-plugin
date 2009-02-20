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
import java.io.PrintWriter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.ClasspathBuilder;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Implementation note :
 * <p>
 * Most of GWT commands require to fork a JVM. The AbstractScriptWriter creates platform dependent command scripts to
 * run them, with required setup to match the project configuration and dependencies.
 * <p>
 * A surefire-like forkBooter is used to setup the required classpath in the forked JVM using a nexted ClassLoader, but
 * this does not work to launch the GWTShell (http://code.google.com/p/google-web-toolkit/issues/detail?id=1032).
 * <p>
 * The option to use a booter Jar (see ClasspathBuilder.createBooter) does not work : GWT fails with error
 * "No source path entries; expect subsequent failures".
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public abstract class AbstractScriptWriter
    extends AbstractLogEnabled
    implements ScriptWriter
{

    /**
     * @param buildClasspathUtil
     */
    public AbstractScriptWriter( ClasspathBuilder buildClasspathUtil )
    {
        super();
        this.buildClasspathUtil = buildClasspathUtil;
    }

    protected ClasspathBuilder buildClasspathUtil;

    protected File file;

    protected PrintWriter writer;

    /**
     * @return the platform "CLASSPATH" variable String substitution
     */
    protected abstract String getPlatformClasspathVariable();

    /**
     * @return the platform "CLASSPATH" variable definition String
     */
    protected abstract String getPlatformClasspathVariableDefinition();

    /**
     * @return the platform script file extension
     */
    protected abstract String getScriptExtension();

    /**
     * Setup a new command Script, with adequate platform declarations for classpath
     */
    protected abstract void createScript( final GwtShellScriptConfiguration config, File file )
        throws MojoExecutionException;

    protected abstract String getExtraJvmArgs( GwtShellScriptConfiguration configuration );



    /**
     * Setup a new command Script, with adequate platform declarations for classpath
     */
    public void createScript( final GwtShellScriptConfiguration config, String name )
        throws MojoExecutionException
    {
        this.file = new File( config.getBuildDir(), name + getScriptExtension() );
        createScript( config, file );
    }

    private void ensureTargetPackageExists( File generateDirectory, String targetName )
    {
        targetName = targetName.substring( 0, targetName.lastIndexOf( '.' ) );
        String targetPackage = targetName.replace( '.', File.separatorChar );
        getLogger().debug(
                           "ensureTargetPackageExists, targetName : " + targetName + ", targetPackage : "
                               + targetPackage );
        File targetPackageDirectory = new File( generateDirectory, targetPackage );
        if ( !targetPackageDirectory.exists() )
        {
            targetPackageDirectory.mkdirs();
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptWriter#executeClass(org.codehaus.mojo.gwt.shell.CompileMojo,
     *      org.codehaus.mojo.gwt.GwtRuntime, int, java.lang.String)
     */
    public void executeClass( GwtShellScriptConfiguration configuration, GwtRuntime runtime, int strategy,
                              String clazz )
        throws MojoExecutionException
    {
        String extra = getExtraJvmArgs( configuration );
        writer.print( "\"" + getJavaCommand( configuration ) + "\" " + extra );

        switch ( strategy )
        {
            case CLASSPATH:
                buildClasspathUtil.writeClassPathVariable( configuration, file, Artifact.SCOPE_RUNTIME, runtime,
                                                           writer, getPlatformClasspathVariableDefinition() );
                writer.print( " -cp \"" + getPlatformClasspathVariable() + "\" " );
                writer.print( clazz );
                break;

            case FORKBOOTER:
            default:
                File classpath = buildClasspathUtil.writeClassPathFile( configuration, runtime );

                writer.print( " -cp \"" + configuration.getPluginJar() + "\" " );
                writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
                writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
                writer.print( clazz );
                break;
        }
    }

    protected String getJavaCommand( GwtShellScriptConfiguration configuration )
        throws MojoExecutionException
    {
        String jvm = configuration.getJvm();
        if ( !StringUtils.isEmpty( jvm ) )
        {
            // does-it exists ? is-it a directory or a path to a java executable ?
            File jvmFile = new File( jvm );
            if ( !jvmFile.exists() )
            {
                throw new MojoExecutionException( "the configured jvm " + jvm
                    + " doesn't exists please check your environnement" );
            }
            if ( jvmFile.isDirectory() )
            {
                // it's a directory we construct the path to the java executable
                return jvmFile.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
            }
            return jvm;

        }
        // use the same JVM as the one used to run Maven (the "java.home" one)
        return System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
    }

    public void print( String s )
    {
        writer.print( s );
    }

    public void println()
    {
        writer.println();
    }

    public void println( String x )
    {
        writer.println( x );
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptWriter#getExecutable()
     */
    public File getExecutable()
    {
        writer.flush();
        writer.close();
        writer = null;
        return file;
    }
}