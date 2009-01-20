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
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.plexus.util.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Goal which install GWT artifacts in local repository.
 * 
 * @goal eclipse
 * @execute phase=generate-resources
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class EclipseMojo
    extends AbstractGwtModuleMojo
{
    /**
     * @component
     */
    private EclipseUtil eclipseUtil;

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
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File outputDirectory;

    /**
     * Additional parameters to append to the module URL. For example, gwt-log users will set "log_level=DEBUG"
     *
     * @parameter
     */
    private String additionalPageParameters;

    /**
     * @param parameters additional parameter for module URL
     */
    public void setAdditionalPageParameters( String parameters )
    {
        // escape the '&' char used for multiple parameters as the result must be XML compliant
        this.additionalPageParameters = StringUtils.replace( parameters, "&", "&amp;" );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        GwtRuntime runtime = getGwtRuntime();
        for ( String module : getModules() )
        {
            createLaunchConfigurationForHostedModeBrowser( runtime, module );
        }
    }

    /**
     * create an Eclipse launch configuration file to Eclipse to run the module in hosted browser
     * @param module the GWT module
     * @throws MojoExecutionException some error occured
     */
    private void createLaunchConfigurationForHostedModeBrowser( GwtRuntime runtime, String module )
        throws MojoExecutionException
    {

        File launchFile = new File( getProject().getBasedir(), module + ".launch" );
        if ( launchFile.exists() )
        {
            getLog().info( "launch file exists " + launchFile.getName() + " skip generation "  );
            return;
        }

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading( EclipseMojo.class, "" );

        Map < String, Object > context = new HashMap < String, Object > ();
        // Read compileSourceRoots from executedProject to retrieve generated source directories
        List<String> sources = executedProject.getCompileSourceRoots();
        context.put( "sources", sources );
        context.put( "module", module );
        int idx = module.lastIndexOf( '.' );
        String page = module.substring( idx + 1 ) + ".html";
        if ( additionalPageParameters != null )
        {
            page += "?" + additionalPageParameters;
        }
        context.put( "page", page );
        int basedir = getProject().getBasedir().getAbsolutePath().length();
        context.put( "out", outputDirectory.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "project", eclipseUtil.getProjectName( getProject() ) );
        context.put( "gwtDevJarPath", runtime.getGwtDevJar().getAbsolutePath() );

        try
        {
            Writer configWriter = new FileWriter( launchFile );
            Template template = cfg.getTemplate( "launch.fm" );
            template.process( context, configWriter );
            configWriter.flush();
            configWriter.close();
            getLog().info( "Write launch configuration for GWT module : " + launchFile.getAbsolutePath() );
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

}
