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
import java.util.HashMap;
import java.util.Map;

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
     * @return a reference to the "CLASSPATH" variable, including the -cp switch.
     */
    protected abstract String getPlatformClasspathVariableReference();

    
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

    private Map<String, String> variables = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptWriter#addVariable(java.lang.String, java.lang.String)
     */
    public void addVariable( String string, String absolutePath )
    {
        variables.put( string, absolutePath );
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptWriter#executeClass(org.codehaus.mojo.gwt.shell.CompileMojo,
     *      org.codehaus.mojo.gwt.GwtRuntime, ClasspathStrategy, java.lang.String)
     */
    public void executeClass( GwtShellScriptConfiguration configuration, GwtRuntime runtime,
                              ClasspathStrategy strategy,
                              String clazz )
        throws MojoExecutionException
    {
        switch ( strategy )
        {
            case CLASSPATH_VARIABLE:
                buildClasspathUtil.writeClassPathVariable( configuration, file, Artifact.SCOPE_RUNTIME, runtime,
                                                           writer, getPlatformClasspathVariableDefinition() );
                javaCommand( configuration );
                writer.print( getPlatformClasspathVariableReference());
                writer.print( clazz );
                break;

            case JARBOOTER:
                File booter = buildClasspathUtil.createBooterJar( configuration, runtime, null, clazz );
                javaCommand( configuration );
                writer.print( " -jar \"" + booter.getAbsolutePath() + "\" " );
                break;

            case FORKBOOTER:
            default:
                File classpath = buildClasspathUtil.writeClassPathFile( configuration, runtime );
                javaCommand( configuration );
                writer.print( " -cp " );
                writer.print( "\"" + configuration.getPluginJar() + "\"" );
                writer.print( File.pathSeparator );
                // gwt-dev is required in bootclasspath to run GWT 1.6 parallel permutation processing
                writer.print( "\"" + runtime.getGwtDevJar() + "\"" );
                writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
                writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
                writer.print( clazz );
                break;
        }
    }

    private void javaCommand( GwtShellScriptConfiguration configuration )
        throws MojoExecutionException
    {
        String extra = getExtraJvmArgs( configuration );
        writer.print( "\"" + getJavaCommand( configuration ) + "\" " );
        writer.print( extra );
        writer.print( " " );
        for ( Map.Entry<String, String> variable : variables.entrySet() )
        {
            writer.append( " -D" );
            writer.append( variable.getKey() );
            writer.append( "=" );
            writer.append( variable.getValue() );
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