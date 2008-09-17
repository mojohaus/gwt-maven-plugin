package org.codehaus.mojo.gwt;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.Commandline;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Execute the i18nCreator tool
 * 
 * @goal i18n
 * @phase generate-sources
 * @see http://code.google.com/webtoolkit/documentation/com.google.gwt.doc.DeveloperGuide.Fundamentals.html#i18nCreator
 * @author ndeloof
 */
public class I18NCreatorMojo
    extends AbstractGwtMojo
{
    /**
     * The messages ResourceBundle used to generate the GWT i18n inteface
     * 
     * @parameter
     * @required
     */
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
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running I18NSync to generate message bundles from " + resourceBundle );

        String jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        List<String> classpath = new ArrayList<String>();
        classpath.addAll( getProject().getCompileSourceRoots() );
        List<Resource> resources = getProject().getResources();
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

            Commandline cli = new Commandline();
            cli.setExecutable( jvm );
            cli.createArg( false ).setLine( "-classpath" );
            cli.createArg( false ).setLine( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            cli.createArg( false ).setLine( "com.google.gwt.i18n.tools.I18NSync" );
            cli.createArg( false ).setLine( "-out" );
            generateDirectory.mkdirs();
            cli.createArg( false ).setLine( generateDirectory.getAbsolutePath() );
            if ( constantsWithLookup )
            {
                cli.createArg( false ).setLine( "-createConstantsWithLookup" );
            }
            if ( messages )
            {
                cli.createArg( false ).setLine( "-createMessages" );
            }
            cli.createArg( false ).setLine( resourceBundle );

            getLog().debug( "execute : " + cli.toString() );
            StreamConsumer systemOut = new ForkingStreamConsumer( new StandardOutputConsumer() );
            StreamConsumer systemErr = new ForkingStreamConsumer( new StandardOutputConsumer() );
            int status = CommandLineUtils.executeCommandLine( cli, systemOut, systemErr, 10000 );
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

}
