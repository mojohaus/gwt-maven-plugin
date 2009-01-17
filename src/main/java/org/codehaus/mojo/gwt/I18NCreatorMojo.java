package org.codehaus.mojo.gwt;

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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.Commandline;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Execute the i18nCreator tool
 * 
 * @deprecated will be replaced with gw-maven I18NMojo
 * @goal i18n
 * @phase generate-sources
 * @see http://code.google.com/webtoolkit/documentation/com.google.gwt.doc.DeveloperGuide.Fundamentals.html#i18nCreator
 * @author ndeloof
 * @version $Id$
 */
public class I18NCreatorMojo
    extends AbstractGwtMojo
{
    /**
     * The messages ResourceBundles used to generate the GWT i18n inteface
     *
     * @parameter
     */
    private String[] resourceBundles;

    /**
     * Timeout for i18nCreator execution in a dedicated JVM
     * @parameter default-value="10000"
     */
    private int timeOut;

    /**
     * Shortcut for a single resourceBundle
     * 
     * @parameter
     */
    @SuppressWarnings( "unused" )
    private String resourceBundle;


    /**
     * If true, create scripts for a ConstantsWithLookup interface rather than a Constants one
     * 
     * @parameter expression="gwt.createConstantsWithLookup"
     */
    private boolean constantsWithLookup;

    /**
     * If true, create scripts for a Messages interface rather than a Constants one
     *
     * @parameter default-value="true" expression="gwt.createMessages"
     */
    private boolean messages;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        for ( String bundle : resourceBundles )
        {
            runI18NSync( bundle );
        }
    }

    /**
     * @param bundle the message bundle to convert to i18n interface
     * @throws MojoExecutionException some error occured
     */
    private void runI18NSync( String bundle )
        throws MojoExecutionException
    {
        getLog().info( "Running I18NSync to generate message bundles from " + bundle );

        String jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        List < String > classpath = new ArrayList < String > ();
        classpath.addAll( getProject().getCompileSourceRoots() );
        List < Resource > resources = getProject().getResources();
        for ( Resource resource : resources )
        {
            classpath.add( resource.getDirectory() );
        }

        URL[] pluginClasspath = ( (URLClassLoader) getClass().getClassLoader() ).getURLs();
        for ( URL url : pluginClasspath )
        {
            classpath.add( url.getFile() );
        }

        try
        {
            // We need to fork a process as I18NSync uses the system classloader to load the resourceBundle,
            // so we have no option to reconfigure the classloader (as CompileMojo does)
            // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2894

            Commandline cli = new Commandline();
            cli.setExecutable( jvm );
            cli.createArg( false ).setLine( "-classpath" );
            cli.createArg( false ).setLine( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            cli.createArg( false ).setLine( "com.google.gwt.i18n.tools.I18NSync" );
            cli.createArg( false ).setLine( "-out" );
            File file = new File( generateDirectory, bundle.replace( '.', File.separatorChar ) );
            file.getParentFile().mkdirs();
            cli.createArg( false ).setLine( generateDirectory.getAbsolutePath() );
            if ( constantsWithLookup )
            {
                cli.createArg( false ).setLine( "-createConstantsWithLookup" );
            }
            if ( messages )
            {
                cli.createArg( false ).setLine( "-createMessages" );
            }
            cli.createArg( false ).setLine( bundle );

            getLog().debug( "execute : " + cli.toString() );
            StreamConsumer systemOut = new ForkingStreamConsumer( new StandardOutputConsumer() );
            StreamConsumer systemErr = new ForkingStreamConsumer( new StandardOutputConsumer() );
            int status = CommandLineUtils.executeCommandLine( cli, systemOut, systemErr, timeOut );
            if ( status != 0 )
            {
                throw new MojoExecutionException( "Failed to run I18NSync : returned " + status );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to run I18NSync", e );
        }
    }

    /**
     * Maven Hack : resourceBundle attribute is used to declare the parameter, but plexus will use
     * the setter to inject value.
     * @param resourceBundle the single bundle to process
     */
    public void setResourceBundle( String resourceBundle )
    {
        this.resourceBundles = new String[] { resourceBundle };
    }    

}
