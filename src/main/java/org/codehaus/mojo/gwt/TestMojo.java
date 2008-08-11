package org.codehaus.mojo.gwt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.Commandline;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * @goal test
 * @phase integration-test
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @requiresDependencyResolution test
 */
public class TestMojo
    extends AbstractGwtMojo
{
    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     * 
     * @parameter expression="${skipTests}"
     */
    private boolean skipTests;

    /**
     * DEPRECATED This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec. Use
     * -DskipTests instead; it's shorter.
     * 
     * @deprecated
     * @parameter expression="${maven.test.skip.exec}"
     */
    private boolean skipExec;

    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the skipTests parameter instead.
     * 
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * @parameter default-value="target/www-test"
     */
    private String out;

    /**
     * @parameter default-value="60"
     */
    private int timeOut;

    /**
     * @parameter default-value="**\/*GwtTest.java"
     */
    private String includes;

    /**
     * @parameter
     */
    private String excludes;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip || skipTests || skipExec )
        {
            return;
        }

        List<String> classpath;
        try
        {
            classpath = new ArrayList<String>( getProject().getTestClasspathElements() );;
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies", e );
        }

        classpath.addAll( getProject().getCompileSourceRoots() );
        classpath.addAll( getProject().getTestCompileSourceRoots() );

        String jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        getLog().debug( "Using JVM: " + jvm );

        List<String> tests = new ArrayList<String>();
        for ( String root : (List<String>) getProject().getTestCompileSourceRoots() )
        {
            if ( !new File( root ).exists() )
            {
                continue;
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( new File( root ) );
            scanner.setIncludes( includes.split( "," ) );
            if ( excludes != null && !"".equals( excludes ) )
            {
                scanner.setExcludes( excludes.split( "," ) );
            }
            scanner.scan();
            String[] files = scanner.getIncludedFiles();
            for ( String file : files )
            {
                tests.add( file );
            }
        }

        banner();
        for ( String test : tests )
        {
            forkToRunTest( classpath, jvm, test );
        }

        // TODO create a surefire-like report (code reuse ?)

        if ( failures > 0 )
        {
            throw new MojoExecutionException( "There was test failures." );
        }

    }

    private int failures;

    /**
     * @param classpath
     * @param jvm
     * @param test
     * @throws MojoExecutionException
     */
    private void forkToRunTest( List<String> classpath, String jvm, String test )
        throws MojoExecutionException
    {
        test = test.substring( 0, test.length() - 5 );
        test = StringUtils.replace( test, File.separator, "." );
        System.out.println( "Running " + test );
        try
        {
            Commandline cli = new Commandline();
            cli.setExecutable( jvm );
            cli.createArg( false ).setLine( "-classpath" );
            cli.createArg( false ).setLine( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            cli.createArg( false ).setLine( " -Xmx256M " );
            cli.createArg( false ).setLine( " -Dgwt.args=\"-out " + out + "\"" );
            new File( getProject().getBasedir(), out ).mkdirs();
            cli.createArg( false ).setLine( " junit.textui.TestRunner " );
            cli.createArg( false ).setLine( test );

            StreamConsumer systemOut = new ForkingStreamConsumer( new StandardOutputConsumer() );
            StreamConsumer systemErr = new ForkingStreamConsumer( new StandardOutputConsumer() );
            if ( CommandLineUtils.executeCommandLine( cli, systemOut, systemErr, timeOut ) != 0 )
            {
                failures++;
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to run GWT tests", e );
        }
    }

    private void banner()
    {
        System.out.println();
        System.out.println( "-------------------------------------------------------" );
        System.out.println( "T E S T S" );
        System.out.println( "-------------------------------------------------------" );
    }

}
