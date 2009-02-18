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
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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
     * @plexus.requirement
     */
    protected ClasspathBuilder buildClasspathUtil;

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
     * @param configuration GwtShell configuration
     * @return JVM arguments based on both platform and shell configuration
     */
    protected abstract String getExtraJvmArgs( GwtShellScriptConfiguration configuration );

    /**
     * Setup a new command Script, with adequate platform declarations for classpath
     */
    protected abstract PrintWriter createScript( final GwtShellScriptConfiguration mojo, File file )
        throws MojoExecutionException;

    /**
     * Write debug script.
     */
    public final File writeDebugScript( DebugScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        return writeRunScript( configuration, configuration.getDebugPort(), configuration.isDebugSuspend(), runtime );
    }

    /**
     * Write run script.
     */
    public final File writeRunScript( RunScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        return writeRunScript( configuration, -1, false, runtime );
    }

    /**
     * Write run script.
     */
    private File writeRunScript( RunScriptConfiguration configuration, int debugPort, boolean debugSuspend,
                                 GwtRuntime runtime )
        throws MojoExecutionException
    {
        String filename = ( debugPort >= 0 ) ? "debug" + getScriptExtension() : "run" + getScriptExtension();
        File file = new File( configuration.getBuildDir(), filename );
        PrintWriter writer = this.createScript( configuration, file );
        // The shell runner does not support ForkBooter strategy to bypass windows command line limit
        // as it uses the System ClassLoader
        buildClasspathUtil.writeClassPathVariable( configuration, file, Artifact.SCOPE_RUNTIME, runtime, writer,
                                                   getPlatformClasspathVariableDefinition() );

        String extra = getExtraJvmArgs( configuration );
        writer.print( "\"" + getJavaCommand( configuration ) + "\" " + extra );

        writer.print( " -Dcatalina.base=\"" + configuration.getTomcat().getAbsolutePath() + "\" " );
        writer.print( " -cp \"" + getPlatformClasspathVariable() + "\" " );
        // writer.print( " -jar \"" + booterJar + "\" " );

        if ( debugPort >= 0 )
        {
            writer.print( " -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=" );
            writer.print( debugPort );
            if ( debugSuspend )
            {
                writer.print( ",suspend=y " );
            }
        }

        // writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
        // writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
        writer.print( " " + runtime.getVersion().getShellFQCN() );
        writer.print( " -gen \"" );
        writer.print( configuration.getGen().getAbsolutePath() );
        writer.print( "\" -logLevel " );
        writer.print( configuration.getLogLevel() );
        writer.print( " -style " );
        writer.print( configuration.getStyle() );
        writer.print( " -port " );
        writer.print( Integer.toString( configuration.getPort() ) );
        if ( configuration.isNoServer() )
        {
            writer.print( " -noserver " );
        }

        switch ( runtime.getVersion() )
        {
            case ONE_DOT_FOUR:
            case ONE_DOT_FIVE:
                writer.print( " -out " );
                writer.print( "\"" + configuration.getOutput().getAbsolutePath() + "\"" );
                writer.print( " " + configuration.getRunTarget() );
                break;
            default:
                writer.print( " -startupUrl " );
                writer.print( "\"" + configuration.getStartupUrl() + "\"" );
                writer.print( " " + configuration.getRunModule() );
                break;
        }

        writer.println();
        writer.flush();
        writer.close();

        return file;
    }

    /**
     * Write compile script.
     */
    public File writeCompileScript( CompileScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        File file = new File( configuration.getBuildDir(), "compile" + getScriptExtension() );
        PrintWriter writer = this.createScript( configuration, file );
        File classpath = buildClasspathUtil.writeClassPathFile( configuration, runtime );
        for ( String target : configuration.getModules() )
        {
            String extra = getExtraJvmArgs( configuration );
            writer.print( "\"" + getJavaCommand( configuration ) + "\" " + extra );
            writer.print( " -cp \"" + configuration.getPluginJar() + "\" " );
            writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
            writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
            writer.print( runtime.getVersion().getCompilerFQCN() );
            writer.print( " -gen \"" );
            writer.print( configuration.getGen().getAbsolutePath() );
            writer.print( "\" -logLevel " );
            writer.print( configuration.getLogLevel() );
            writer.print( " -style " );
            writer.print( configuration.getStyle() );

            switch ( runtime.getVersion() )
            {
                case ONE_DOT_FOUR:
                case ONE_DOT_FIVE:
                    writer.print( " -out " );
                    writer.print( "\"" + configuration.getOutput().getAbsolutePath() + "\"" );
                    break;
                default:
                    writer.print( " -war " );
                    writer.print( "\"" + configuration.getOutput().getAbsolutePath() + "\"" );
                    writer.print( " -localWorkers " );
                    writer.print( Runtime.getRuntime().availableProcessors() );
                    break;
            }

            writer.print( " " );

            if ( configuration.isEnableAssertions() )
            {
                writer.print( " -ea " );
            }

            writer.print( target );
            writer.println();
        }

        writer.flush();
        writer.close();

        return file;
    }

    /**
     * Write i18n script.
     */
    public File writeI18nScript( I18nScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        File file = new File( configuration.getBuildDir(), "i18n" + getScriptExtension() );
        if ( !file.exists() )
        {
            getLogger().debug( "File '" + file.getAbsolutePath() + "' does not exsists, trying to create." );
            try
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
                getLogger().debug( "New file '" + file.getAbsolutePath() + "' created." );
            }
            catch ( Exception exe )
            {
                getLogger().error(
                                   "Couldn't create file '" + file.getAbsolutePath() + "'. Reason: " + exe.getMessage(),
                                   exe );
            }
        }
        PrintWriter writer = this.createScript( configuration, file );
        File classpath = buildClasspathUtil.writeClassPathFile( configuration, runtime );
        // constants
        if ( configuration.getI18nConstantsBundles() != null )
        {
            for ( String target : configuration.getI18nConstantsBundles() )
            {
                ensureTargetPackageExists( configuration.getGenerateDirectory(), target );
                String extra = getExtraJvmArgs( configuration );
                writer.print( "\"" + getJavaCommand( configuration ) + "\" " + extra );
                writer.print( " -cp \"" + configuration.getPluginJar() + "\" " );
                writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
                writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
                writer.print( " com.google.gwt.i18n.tools.I18NSync" );
                writer.print( " -out " );
                writer.print( "\"" + configuration.getGenerateDirectory() + "\"" );
                writer.print( " " );
                // gwt I18n failed if target package doesn't exists on file system
                writer.print( target );
                writer.println();
            }
        }

        // messages
        if ( configuration.getI18nMessagesBundles() != null )
        {
            for ( String target : configuration.getI18nMessagesBundles() )
            {
                ensureTargetPackageExists( configuration.getGenerateDirectory(), target );
                String extra = ( configuration.getExtraJvmArgs() != null ) ? configuration.getExtraJvmArgs() : "";

                writer.print( "\"" + getJavaCommand( configuration ) + "\" " + extra );
                writer.print( " -cp \"" + configuration.getPluginJar() + "\" " );
                writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
                writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
                writer.print( " com.google.gwt.i18n.tools.I18NSync" );
                writer.print( " -createMessages " );
                writer.print( " -out " );
                writer.print( "\"" + configuration.getGenerateDirectory() + "\"" );
                writer.print( " " );
                writer.print( target );
                writer.println();
            }
        }

        // TODO support getI18nConstantsWithLookupBundles

        writer.flush();
        writer.close();

        return file;
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
     * Write test scripts.
     */
    public void writeTestScripts( TestScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {

        // get extras
        String extra = getExtraJvmArgs( configuration );
        String testExtra = configuration.getExtraTestArgs() != null ? configuration.getExtraTestArgs() : "";

        // make sure output dir is present
        File outputDir = new File( configuration.getBuildDir(), "gwtTest" );
        outputDir.mkdirs();
        outputDir.mkdir();

        File classpath = buildClasspathUtil.writeClassPathFile( configuration, runtime );
        // for each test compile source root, build a test script
        List<String> testCompileRoots = configuration.getProject().getTestCompileSourceRoots();
        for ( String currRoot : testCompileRoots )
        {
            Collection<File> coll =
                FileUtils.listFiles( new File( currRoot ), new WildcardFileFilter( configuration.getTestFilter() ),
                                     HiddenFileFilter.VISIBLE );
            for ( File currFile : coll )
            {
                String testName = currFile.toString();
                getLogger().debug( ( "gwtTest test match found (after filter applied) - " + testName ) );

                // parse off the extension
                if ( testName.lastIndexOf( '.' ) > testName.lastIndexOf( File.separatorChar ) )
                {
                    testName = testName.substring( 0, testName.lastIndexOf( '.' ) );
                }
                if ( testName.startsWith( currRoot ) )
                {
                    testName = testName.substring( currRoot.length() );
                }
                if ( testName.startsWith( File.separator ) )
                {
                    testName = testName.substring( 1 );
                }
                testName = StringUtils.replace( testName, File.separatorChar, '.' );
                getLogger().debug( "testName after parsing - " + testName );

                // start script inside gwtTest output dir, and name it with test class name
                File file =
                    new File( configuration.getBuildDir() + File.separator + "gwtTest", "gwtTest-" + testName
                        + getScriptExtension() );
                PrintWriter writer = this.createScript( configuration, file );

                // build Java command
                writer.print( "\"" + getJavaCommand( configuration ) + "\" " );
                if ( extra.length() > 0 )
                {
                    writer.print( " " + extra + " " );
                }
                if ( testExtra.length() > 0 )
                {
                    writer.print( " " + testExtra + " " );
                }
                writer.print( " -cp \"" + configuration.getPluginJar() + "\" " );
                writer.print( " org.codehaus.mojo.gwt.fork.ForkBooter " );
                writer.print( " \"" + classpath.getAbsolutePath() + "\" " );
                writer.print( "junit.textui.TestRunner " );
                writer.print( testName );

                // write script out
                writer.flush();
                writer.close();
            }
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

}