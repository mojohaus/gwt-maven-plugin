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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.shell.PlatformUtil;
import org.codehaus.plexus.util.StringUtils;

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
   /** Creates a new instance of ScriptWriterUnix */
   public ScriptWriterUnix() {
   }

    /**
     * Write debug script.
     */
    public File writeDebugScript( DebugScriptConfiguration configuration )
        throws MojoExecutionException
    {
        return writeRunScript( configuration, configuration.getDebugPort(), configuration.isDebugSuspend() );
    }

    /**
     * Write run script.
     */
   public File writeRunScript( RunScriptConfiguration configuration )
       throws MojoExecutionException
    {
        return writeRunScript( configuration, -1, false );
    }

    /**
     * Write run script.
     */
    private File writeRunScript( RunScriptConfiguration configuration, int debugPort, boolean debugSuspend )
        throws MojoExecutionException
    {
      String filename = ( debugPort >= 0 ) ? "debug.sh" : "run.sh";
      File file = new File(configuration.getBuildDir(), filename);
      PrintWriter writer = this.getPrintWriterWithClasspath(configuration, file, Artifact.SCOPE_RUNTIME);

      String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
      if ( PlatformUtil.OS_NAME.startsWith( "mac" ) && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
      {
         extra = "-XstartOnFirstThread " + extra;
      }

      writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp $CLASSPATH " );

      if ( debugPort >= 0 )
      {
         writer.print(" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=");
         writer.print( debugPort );
         writer.print( debugSuspend ? ",suspend=y " : ",suspend=n " );
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

      this.chmodUnixFile(file);
      return file;
   }

   /**
    * Write compile script.
    */
   public File writeCompileScript( CompileScriptConfiguration configuration )
        throws MojoExecutionException
    {
      File file = new File(configuration.getBuildDir(), "compile.sh");
      PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_COMPILE );

      for (String target : configuration.getCompileTarget()) {

         String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
         if ( PlatformUtil.OS_NAME.startsWith( "mac" ) && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
            {
            extra = "-XstartOnFirstThread " + extra;
         }

         writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp $CLASSPATH " );
         writer.print(" com.google.gwt.dev.GWTCompiler ");
         writer.print(" -gen ");
         writer.print(configuration.getGen().getAbsolutePath());
         writer.print(" -logLevel ");
         writer.print(configuration.getLogLevel());
         writer.print(" -style ");
         writer.print(configuration.getStyle());
         writer.print(" -out ");

         if (configuration.isEnableAssertions()) {
            writer.print(" -ea ");
         }

         writer.print(configuration.getOutput().getAbsolutePath());
         writer.print(" ");
         writer.print(target);
         writer.println();
      }

      writer.flush();
      writer.close();

      this.chmodUnixFile(file);
      return file;
   }

    /**
     * Write i18n script.
     */
   public File writeI18nScript( I18nScriptConfiguration configuration )
        throws MojoExecutionException
    {

      File file = new File(configuration.getBuildDir(), "i18n.sh");
      if (!file.exists()) {
         if (configuration.getLog().isDebugEnabled())
            configuration.getLog().debug("File '" + file.getAbsolutePath() + "' does not exsists, trying to create.");
         try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            if (configuration.getLog().isDebugEnabled())
               configuration.getLog().debug("New file '" + file.getAbsolutePath() + "' created.");
         }
         catch (Exception exe) {
            configuration.getLog().error("Couldn't create file '" + file.getAbsolutePath() + "'. Reason: " + exe.getMessage(),
                     exe);
         }
      }
      PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_COMPILE );

      // constants
      if (configuration.getI18nConstantsNames() != null) {
         for (String target : configuration.getI18nConstantsNames()) {
            String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
            if ( PlatformUtil.OS_NAME.startsWith( "mac" )
                    && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
                {
               extra = "-XstartOnFirstThread " + extra;
            }

            writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp $CLASSPATH" );
            writer.print(" com.google.gwt.i18n.tools.I18NSync");
            writer.print(" -out ");
            writer.print(configuration.getI18nOutputDir());
            writer.print(" ");
            writer.print(target);
            writer.println();
         }
      }

      // messages
      if (configuration.getI18nMessagesNames() != null) {
         for (String target : configuration.getI18nMessagesNames()) {
            String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
            if ( PlatformUtil.OS_NAME.startsWith( "mac" )
                    && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
                {
               extra = "-XstartOnFirstThread " + extra;
            }

            writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " + extra + " -cp $CLASSPATH" );
            writer.print(" com.google.gwt.i18n.tools.I18NSync");
            writer.print(" -createMessages ");
            writer.print(" -out ");
            writer.print(configuration.getI18nOutputDir());
            writer.print(" ");
            writer.print(target);
            writer.println();
         }
      }

      writer.flush();
      writer.close();

      this.chmodUnixFile(file);
      return file;
   }

   /**
    * Write test scripts.
    */
   public void writeTestScripts( TestScriptConfiguration configuration )
        throws MojoExecutionException
    {

      // get extras
      String extra = (configuration.getExtraJvmArgs() != null) ? configuration.getExtraJvmArgs() : "";
      if ( PlatformUtil.OS_NAME.startsWith( "mac" ) && ( extra.indexOf( "-XstartOnFirstThread" ) == -1 ) )
        {
         extra = "-XstartOnFirstThread " + extra;
      }
      String testExtra = configuration.getExtraTestArgs() != null ? configuration.getExtraTestArgs() : "";

      // make sure output dir is present
      File outputDir = new File(configuration.getBuildDir(), "gwtTest");
      outputDir.mkdirs();
      outputDir.mkdir();

      // for each test compile source root, build a test script
      List<String> testCompileRoots = configuration.getProject().getTestCompileSourceRoots();
      for (String currRoot : testCompileRoots) {

         // TODO better file filter here
         Collection<File> coll = FileUtils.listFiles(new File(currRoot), new WildcardFileFilter(configuration.getTestFilter()),
                  HiddenFileFilter.VISIBLE);

         for (File currFile : coll) {

            String testName = currFile.toString();
            configuration.getLog().debug(("gwtTest test match found (after filter applied) - " + testName));

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
            configuration.getLog().debug("testName after parsing - " + testName);

            // start script inside gwtTest output dir, and name it with test class name
            File file = new File(configuration.getBuildDir() + File.separator + "gwtTest", "gwtTest-" + testName + ".sh");
            PrintWriter writer = this.getPrintWriterWithClasspath( configuration, file, Artifact.SCOPE_TEST );

            // build Java command
                writer.print( "\"" + PlatformUtil.JAVA_COMMAND + "\" " );
            if (extra.length() > 0) {
               writer.print(" " + extra + " ");
            }
            if (testExtra.length() > 0) {
               writer.print(" " + testExtra + " ");
            }
            writer.print(" -cp $CLASSPATH ");
            writer.print("junit.textui.TestRunner ");
            writer.print(testName);

            // write script out
            writer.flush();
            writer.close();
            this.chmodUnixFile(file);
         }
      }
   }

    /**
     * Util to get a PrintWriter with Unix preamble and classpath.
     *
     * @param mojo
     * @param file
     * @return
     * @throws MojoExecutionException
     */
   private PrintWriter getPrintWriterWithClasspath( final GwtShellScriptConfiguration mojo, File file,
                                                     final String scope )
            throws MojoExecutionException {

      PrintWriter writer = null;
      try {
         writer = new PrintWriter(new FileWriter(file));
      }
      catch (IOException e) {
         throw new MojoExecutionException("Error creating script - " + file, e);
      }
      File sh = new File("/bin/bash");

      if (!sh.exists()) {
         sh = new File("/usr/bin/bash");
      }

      if (!sh.exists()) {
         sh = new File("/bin/sh");
      }
      writer.println("#!" + sh.getAbsolutePath());
      writer.println();

      try {
         Collection<File> classpath = buildClasspathUtil.buildClasspathList( mojo, scope );
         writer.print("export CLASSPATH=");
         Iterator it = classpath.iterator();
         while (it.hasNext()) {
            File f = (File) it.next();
            if (it.hasNext())
               writer.print("\"" + f.getAbsolutePath() + "\":");
            else
               writer.print("\"" + f.getAbsolutePath() + "\"");
         }
      }
      catch (DependencyResolutionRequiredException e) {
         throw new MojoExecutionException("Error creating script - " + file, e);
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
   private void chmodUnixFile(File file) {
      try {
         ProcessWatcher pw = new ProcessWatcher("chmod +x " + file.getAbsolutePath());
         pw.startProcess(System.out, System.err);
         pw.waitFor();
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}
