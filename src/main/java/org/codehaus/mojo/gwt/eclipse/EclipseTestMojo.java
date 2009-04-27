package org.codehaus.mojo.gwt.eclipse;

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
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.PlatformUtil;
import org.codehaus.mojo.gwt.test.TestMojo;
import org.codehaus.mojo.gwt.test.TestTemplate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Goal which creates Eclipse lauch configurations for GWTTestCases.
 * 
 * @goal eclipseTest
 * @execute phase=generate-test-resources
 * @version $Id$
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @deprecated use google eclipse plugin http://code.google.com/intl/fr-FR/eclipse/docs/users_guide.html
 */
public class EclipseTestMojo
    extends TestMojo
{
    /**
     * @component
     */
    private EclipseUtil eclipseUtil;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * <p>
     * Can be set from command line using '-Dgwt.extraJvmArgs=...', defaults to setting max Heap size to be large enough
     * for most GWT use cases.
     * 
     * @parameter expression="${gwt.extraJvmArgs}" default-value="-Xmx512m"
     */
    private String extraJvmArgs;

    /**
     * The currently executed project (phase=generate-resources).
     * 
     * @parameter expression="${executedProject}"
     * @readonly
     */
    private MavenProject executedProject;

    /**
     * Location of the file.
     * 
     * @parameter default-value="${project.build.directory}/www-test"
     */
    private File testOutputDirectory;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final GwtRuntime runtime = getGwtRuntime();

        new TestTemplate( getProject(), includes, excludes, new TestTemplate.CallBack()
        {
            public void doWithTest( File sourceDir, String test )
                throws MojoExecutionException
            {
                createLaunchConfigurationForGwtTestCase( runtime, sourceDir, test );
            }
        } );
    }

    /**
     * Create an eclipse launch configuration file for the specified test
     * @param test the GWTTestCase
     * @param testSrc the source directory where the test lives
     * @throws MojoExecutionException some error occured
     */
    private void createLaunchConfigurationForGwtTestCase( GwtRuntime runtime, File testSrc, String test )
        throws MojoExecutionException
    {
        File testFile = new File( testSrc, test );

        String fqcn = test.replace( File.separatorChar, '.' ).substring( 0, test.lastIndexOf( '.' ) );
        File launchFile = new File( getProject().getBasedir(), fqcn + ".launch" );
        if ( launchFile.exists() && launchFile.lastModified() > testFile.lastModified() )
        {
            return;
        }

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading( EclipseTestMojo.class, "" );

        Map < String, Object > context = new HashMap < String, Object > ();
        List < String > sources = new LinkedList < String >();
        sources.addAll( executedProject.getTestCompileSourceRoots() );
        sources.addAll( executedProject.getCompileSourceRoots() );
        context.put( "sources", sources );
        context.put( "test", fqcn );
        int basedir = getProject().getBasedir().getAbsolutePath().length();
        context.put( "out", testOutputDirectory.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "extraJvmArgs", getExtraJvmArgs() );
        context.put( "project", eclipseUtil.getProjectName( getProject() ) );
        context.put( "gwtDevJarPath", runtime.getGwtDevJar().getAbsolutePath() );

        try
        {
            Writer configWriter = new FileWriter( launchFile );
            Template template = cfg.getTemplate( "test-launch.fm" );
            template.process( context, configWriter );
            configWriter.flush();
            configWriter.close();
            getLog().info( "Write launch configuration for GWT test : " + launchFile.getAbsolutePath() );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to write launch configuration", ioe );
        }
        catch ( TemplateException te )
        {
            throw new MojoExecutionException( "Unable to merge freemarker template", te );
        }
    }

    /**
     * @return
     */
    protected String getExtraJvmArgs()
    {
        String extra = extraJvmArgs;
        if ( PlatformUtil.onMac() && !extraJvmArgs.contains( "-XstartOnFirstThread" ) )
        {
            extra += " -XstartOnFirstThread";
        }
        return extra;
    }
}
