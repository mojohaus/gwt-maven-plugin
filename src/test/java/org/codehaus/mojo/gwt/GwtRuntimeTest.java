package org.codehaus.mojo.gwt;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

import junit.framework.TestCase;

/**
 * Check version detection from GWT-dev Jar
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
public class GwtRuntimeTest
    extends TestCase
{
    /**
     * Check version detection from GWT-dev Jar
     * 
     * @throws MojoExecutionException
     */
    public void testGwtVersion16M1Detection()
        throws MojoExecutionException
    {
        File basedir = new File( System.getProperty( "basedir", "." ) );
        File gwtHome = new File( basedir, "target/test-classes/fake-1.6.0" );
        GwtRuntime gwt = new GwtRuntime( gwtHome );
        assertEquals( GwtVersion.ONE_DOT_SIX, gwt.getVersion() );
    }

    public void testGwtVersion153Detection()
        throws MojoExecutionException
    {
        File basedir = new File( System.getProperty( "basedir", "." ) );
        File gwtHome = new File( basedir, "target/test-classes/fake-1.5.3" );
        GwtRuntime gwt = new GwtRuntime( gwtHome );
        assertEquals( GwtVersion.ONE_DOT_FIVE, gwt.getVersion() );
    }

    public void testGwtVersion1462Detection()
        throws MojoExecutionException
    {
        File basedir = new File( System.getProperty( "basedir", "." ) );
        File gwtHome = new File( basedir, "target/test-classes/fake-1.4.62" );
        GwtRuntime gwt = new GwtRuntime( gwtHome );
        assertEquals( GwtVersion.ONE_DOT_FOUR, gwt.getVersion() );
    }

}
