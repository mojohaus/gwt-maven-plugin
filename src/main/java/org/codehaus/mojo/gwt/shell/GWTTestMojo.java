package org.codehaus.mojo.gwt.shell;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.shell.scripting.ScriptUtil;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;
import org.codehaus.mojo.gwt.shell.scripting.TestScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.TestResult.TestCode;

/**
 * Runs special (non surefire) test phase for GWTTestCase derived tests. This is necessary because of several
 * complications with regard to surefire and the classpath for GWTTestCase/JUnitShell. See further notes in source.
 * 
 * @goal gwttest
 * @phase test
 * @requiresDependencyResolution test
 * @description Runs special (non surefire) test phase for GWTTestCase derived tests.
 * @author ccollins
 */
public class GWTTestMojo
    extends AbstractGwtShellMojo
    implements TestScriptConfiguration
{

    /*
     * This is based on the clever work Will Pugh did in the original "gwtTest" stuff. This has been refactored to make
     * it a bit more robust, and to use the same write script approach all the other GWT-Maven mojos use (and those are
     * used because they are easier to tweak and debug than the Maven classpath - unfortunately). Disclaimer: this is a
     * giant hack because Surefire has some issues with GWTTestCase. Surefire states that it offers multiple ways to
     * load the classpath (http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html), but it
     * doesn't seem to work for the plain java class path case. Manifest class path works, and isolated classpath also
     * works, but just getting to a plain java class path does not seem to work (surefire still refers to
     * /tmp/surefireX). Without a plain java class path GWTTestCase won't work - because GWTTestCase inspects the
     * classpath and sets itself up, and it doesn't like anything other than a plain java classpath (doesn't like a
     * manifest jar, or isolated). Also, presuming surefire did work, in plain java class path mode, we would then still
     * be susceptible to the line too long on Windows issue surefire works around with the other modes. A lot of
     * research into just using surefire was done (we don't want to have to offer this special class for GWTTestCase) -
     * after RTFM and trying the useSystemClassLoader and useManifestOnlyJar settings, various ways, we still could
     * never get it to work. These issues are similar to our experiences:
     * http://www.mail-archive.com/users@maven.apache.org/msg87660.html - and
     * http://jira.codehaus.org/browse/SUREFIRE-508. Hopefully we can kill this someday, it's a hack, but for now, this
     * is the ONLY way to run GWTTestCase based tests from an automated Maven build.
     */

   public void execute() throws MojoExecutionException, MojoFailureException {
      if (isTestSkip()) {
         return;
      }

      this.getLog().info("running GWTTestCase tests (using test name filter -  " + this.getTestFilter() + ")");

      FileWriter testResultsWriter = null;

      // build scripts for each test case for the correct platform
      // (note that scripts end up in outputDirectory/gwtTest)
        ScriptWriter writer = scriptWriterFactory.getScriptWriter();
      writer.writeTestScripts(this);

      // run the scripts
      boolean testFailure = false;
      File testDir = new File(this.getBuildDir(), "gwtTest");
      FileFilter fileFilter = new WildcardFileFilter("gwtTest-*");
      File[] files = testDir.listFiles(fileFilter);
      for (int i = 0; i < files.length; i++) {
         File test = files[i];

         // create results writer
         try {
            String outTestName = test.getName();
            outTestName = outTestName.substring(0, test.getName().lastIndexOf(".")); // strip end .sh/cmd
            outTestName = outTestName.substring(8, outTestName.length()); // strip start gwtTest-
            testResultsWriter = new FileWriter(new File(testDir, "TEST-" + outTestName + ".txt"));

            // run test script and capture output
            org.codehaus.mojo.gwt.shell.scripting.TestResult testResult = ScriptUtil.runTestScript(test);

            // if testCode not success, overall must fail build
            if (testResult.code == TestCode.ERROR || testResult.code == TestCode.FAILURE) {
               testFailure = true;
            }

            // write results to result file
            testResultsWriter.write("Test Code - " + testResult.code + "\n");
            testResultsWriter.write("Test Output: \n" + testResult.message + "\n");
            testResultsWriter.flush();
            testResultsWriter.close();

            this.getLog().info(outTestName + " completed, GWTTestCase result: " + testResult.lastLine);
         }
         catch (IOException e) {
            throw new MojoExecutionException("unable to create test results output file", e);
         }
      }

      // after the loop show output and or handle overall failure
        // TODO add up results and show X runs - X successes, X failures, etc

      this.getLog().info("all tests completed - ran " + files.length + " tests - see results in target/gwtTest");

      if (testFailure) {
         throw new MojoExecutionException("There were GWTTestCase test failures - see results in target/gwtTest");
      }
   }
}
