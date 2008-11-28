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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.PlatformUtil;
import org.codehaus.plexus.util.StringUtils;

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

    public ScriptWriterWindows() {
    }

    /**
     * Write debug script.
     */
    public File writeDebugScript( DebugScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        return writeRunScript( configuration, configuration.getDebugPort(), runtime );
    }

    /**
     * Write run script.
     */
    public File writeRunScript( RunScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {
        return writeRunScript( configuration, -1, runtime );
    }

    /**
     * Write run script.
     */
    private File writeRunScript( RunScriptConfiguration configuration, int debugPort, GwtRuntime runtime )
        throws MojoExecutionException
    {
        String filename = ( debugPort >= 0 ) ? "debug.cmd" : "run.cmd";
        File file = new File(configuration.getBuildDir(), filename);
        PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_RUNTIME, runtime );

        String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
        writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp %CLASSPATH% " );

        if ( debugPort >= 0 )
        {
            writer.print(" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=");
            writer.print( debugPort );
            writer.print(",suspend=y ");
        }

        writer.print( " -Dcatalina.base=\"" + configuration.getTomcat().getAbsolutePath() + "\" " );
        writer.print(" com.google.gwt.dev.GWTShell");
        writer.print(" -gen \"");
        writer.print(configuration.getGen().getAbsolutePath());
        writer.print("\" -logLevel ");
        writer.print(configuration.getLogLevel());
        writer.print(" -style ");
        writer.print(configuration.getStyle());
        writer.print(" -out ");
        writer.print("\"" + configuration.getOutput().getAbsolutePath() + "\"");
        writer.print(" -port ");
        writer.print(Integer.toString(configuration.getPort()));

        if (configuration.isNoServer()) {
            writer.print(" -noserver ");
        }

        writer.print(" " + configuration.getRunTarget());
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
        File file = new File(configuration.getBuildDir(), "compile.cmd");
        PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_COMPILE, runtime );

        for (String target : configuration.getModules()) {
            String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
            writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp %CLASSPATH% " );
            writer.print(" com.google.gwt.dev.GWTCompiler ");
            writer.print(" -gen \"");
            writer.print(configuration.getGen().getAbsolutePath());
            writer.print("\" -logLevel ");
            writer.print(configuration.getLogLevel());
            writer.print(" -style ");
            writer.print(configuration.getStyle());

            writer.print(" -out ");
            writer.print("\"" + configuration.getOutput().getAbsolutePath() + "\"");
            writer.print(" ");

            if (configuration.isEnableAssertions()) {
                writer.print(" -ea ");
            }

            writer.print(target);
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
        File file = new File(configuration.getBuildDir(), "i18n.cmd");
        if (!file.exists()) {
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
                                     "Couldn't create file '" + file.getAbsolutePath() + "'. Reason: "
                                         + exe.getMessage(), exe );
            }
        }
        PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_COMPILE, runtime );

        // constants
        if (configuration.getI18nConstantsNames() != null) {
            for (String target : configuration.getI18nConstantsNames()) {
                String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";

                writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp %CLASSPATH%" );
                writer.print(" com.google.gwt.i18n.tools.I18NSync");
                writer.print(" -out ");
                writer.print("\"" + configuration.getI18nOutputDir() + "\"");
                writer.print(" ");
                writer.print(target);
                writer.println();
            }
        }

        // messages
        if (configuration.getI18nMessagesNames() != null) {
            for (String target : configuration.getI18nMessagesNames()) {
                String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";

                writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp %CLASSPATH%" );
                writer.print(" com.google.gwt.i18n.tools.I18NSync");
                writer.print(" -createMessages ");
                writer.print(" -out ");
                writer.print("\"" + configuration.getI18nOutputDir() + "\"");
                writer.print(" ");
                writer.print(target);
                writer.println();
            }
        }

        writer.flush();
        writer.close();

        return file;
    }

    /**
     * Write test scripts.
     */
    public void writeTestScripts( TestScriptConfiguration configuration, GwtRuntime runtime )
        throws MojoExecutionException
    {

        // get extras
        String extra = ( configuration.getExtraJvmArgs() != null ) ? configuration.getExtraJvmArgs() : "";
        String testExtra = configuration.getExtraTestArgs() != null ? configuration.getExtraTestArgs() : "";

        // make sure output dir is present
        File outputDir = new File(configuration.getBuildDir(), "gwtTest");
        outputDir.mkdirs();
        outputDir.mkdir();

        // for each test compile source root, build a test script
        List<String> testCompileRoots = configuration.getProject().getTestCompileSourceRoots();
        for (String currRoot : testCompileRoots) {

            Collection<File> coll = FileUtils.listFiles(new File(currRoot),
                    new WildcardFileFilter(configuration.getTestFilter()), HiddenFileFilter.VISIBLE);

            for (File currFile : coll) {

                String testName = currFile.toString();
                getLogger().debug( ( "gwtTest test match found (after filter applied) - " + testName ) );

                // parse off the extension
                if (testName.lastIndexOf('.') > testName.lastIndexOf(File.separatorChar)) {
                    testName = testName.substring(0, testName.lastIndexOf('.'));
                }
                if (testName.startsWith(currRoot)) {
                    testName = testName.substring(currRoot.length());
                }
                if (testName.startsWith(File.separator)) {
                    testName = testName.substring(1);
                }
                testName = StringUtils.replace(testName, File.separatorChar, '.');
                getLogger().debug( "testName after parsing - " + testName );

                // start script inside gwtTest output dir, and name it with test class name
                File file = new File(configuration.getBuildDir() + File.separator + "gwtTest", "gwtTest-" + testName + ".cmd");
                PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_TEST, runtime );

                // build Java command
                writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " );
                if (extra.length() > 0) {
                    writer.print(" " + extra + " ");
                }
                if (testExtra.length() > 0) {
                    writer.print(" " + testExtra + " ");
                }
                writer.print(" -cp %CLASSPATH% ");
                writer.print("junit.textui.TestRunner ");
                writer.print(testName);

                // write script out
                writer.flush();
                writer.close();
            }
        }
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
    private PrintWriter getPrintWriterWithClasspath( final GwtShellScriptConfiguration config, File file,
                                                     final String scope, GwtRuntime runtime )
            throws MojoExecutionException {

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file));
            writer.println("@echo off");
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating script - " + file, e);
        }

        try {
            Collection<File> classpath =
                buildClasspathUtil.buildClasspathList( config.getProject(), scope, runtime, config.getSourcesOnPath(),
                                                       config.getResourcesOnPath() );
            writer.print("set CLASSPATH=");

            StringBuffer cpString = new StringBuffer();

            for (File f : classpath) {
                cpString.append("\"" + f.getAbsolutePath() + "\";");
                // break the line at 4000 characters to try to avoid max size
                if (cpString.length() > 4000) {
                    writer.println(cpString);
                    cpString = new StringBuffer();
                    writer.print("set CLASSPATH=%CLASSPATH%;");
                }
            }
            writer.println(cpString);
            writer.println();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error creating script - " + file, e);
        }

        writer.println();
        return writer;
    }
}
