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
import static org.codehaus.mojo.gwt.EmbeddedServer.JETTY;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
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
 * Goal which creates Eclipse lauch configurations for GWT modules.
 * 
 * @goal eclipse
 * @execute phase=generate-resources
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @deprecated use google eclipse plugin http://code.google.com/intl/fr-FR/eclipse/docs/users_guide.html
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
     * @parameter default-value="${basedir}/src/main/webapp"
     */
    private File outputDirectory;

    /**
     * Location of the compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @readOnly
     */
    private File buildOutputDirectory;

    /**
     * Location of the hosted-mode web application structure. Default value matches the Google Eclipse plugin.
     *
     * @parameter default-value="${basedir}/war"
     */
    private File hostedWebapp;

    /**
     * Additional parameters to append to the module URL. For example, gwt-log users will set "log_level=DEBUG"
     *
     * @parameter
     */
    private String additionalPageParameters;

    /**
     * Run without hosted mode server
     *
     * @parameter default-value="false" expression="${gwt.noserver}"
     */
    private boolean noserver;

    /**
     * Port of the HTTP server used when noserver is set
     *
     * @parameter default-value="8080" expression="${gwt.port}"
     */
    private int port;

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

        if ( runtime.getVersion().getEmbeddedServer() == JETTY )
        {
            // Jetty requires an exploded webapp
            try
            {
                File classes = new File( hostedWebapp, "WEB-INF/classes" );
                if ( !buildOutputDirectory.getAbsolutePath().equals( classes.getAbsolutePath() ) )
                {
                    getLog().error(
                                    "Your POM <build><outputdirectory> must match your "
                                        + "hosted webapp WEB-INF/classes folder for GWT Hosted browser to see your classes." );
                    throw new MojoExecutionException( "Configuration does not match GWT Hosted mode requirements" );
                }

                File lib = new File( hostedWebapp, "WEB-INF/lib" );
                getLog().info( "create exploded Jetty webapp in " + hostedWebapp );
                lib.mkdirs();

                File basedir = new File( localRepository.getBasedir() );
                Collection<Artifact> artifacts = project.getRuntimeArtifacts();
                for ( Artifact artifact : artifacts )
                {
                    File file = new File( basedir, localRepository.pathOf( artifact ) );
                    FileUtils.copyFileToDirectory( file, lib );
                }

            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( "Failed to create Jetty exploded webapp", ioe );
            }
        }

        for ( String module : getModules() )
        {
            createLaunchConfigurationForHostedModeBrowser( runtime, module );
        }
    }

    /**
     * create an Eclipse launch configuration file to Eclipse to run the module in hosted browser
     *
     * @param module the GWT module
     * @throws MojoExecutionException some error occured
     */
    private void createLaunchConfigurationForHostedModeBrowser( GwtRuntime runtime, String module )
        throws MojoExecutionException
    {
        File launchFile = new File( getProject().getBasedir(), module + ".launch" );
        if ( launchFile.exists() )
        {
            getLog().info( "launch file exists " + launchFile.getName() + " skip generation " );
            return;
        }

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading( EclipseMojo.class, "" );

        Map<String, Object> context = new HashMap<String, Object>();
        // Read compileSourceRoots from executedProject to retrieve generated source directories
        Collection<String> sources = new LinkedList<String>( executedProject.getCompileSourceRoots() );
        List<Resource> resources = executedProject.getResources();
        for ( Resource resource : resources )
        {
            sources.add( resource.getDirectory() );
        }
        context.put( "sources", sources );
        context.put( "module", module );
        context.put( "runtime", runtime );
        context.put( "localRepository", localRepository.getBasedir() );
        int idx = module.lastIndexOf( '.' );
        String page = module.substring( idx + 1 ) + ".html";
        if ( additionalPageParameters != null )
        {
            page += "?" + additionalPageParameters;
        }

        String renameTo = readModule( module ).getRenameTo();
        String modulePath = ( renameTo != null ? renameTo : module );
        context.put( "modulePath", modulePath );

        context.put( "page", page );
        int basedir = getProject().getBasedir().getAbsolutePath().length();
        context.put( "out", outputDirectory.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "war", hostedWebapp.getAbsolutePath().substring( basedir + 1 ) );
        context.put( "additionalArguments", noserver ? "-noserver -port " + port : "" );
        context.put( "project", eclipseUtil.getProjectName( getProject() ) );
        context.put( "gwtDevJarPath", runtime.getGwtDevJar().getAbsolutePath().replace( '\\', '/' ) );

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
