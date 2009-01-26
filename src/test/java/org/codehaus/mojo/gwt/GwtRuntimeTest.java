package org.codehaus.mojo.gwt;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author ndeloof
 */
public class GwtRuntimeTest
    extends TestCase
{
    /**
     * Check version detection from GWT-dev Jar
     */
    public void testGwtVersion153Detection()
    {
        File basedir = new File( System.getProperty( "basedir", "." ) );
        File gwtDevJar = new File( basedir, "target/test-classes/gwt-dev-1.5.3-fake.jar" );
        GwtRuntime gwt = new GwtRuntime( null, gwtDevJar );
        assertEquals( GwtVersion.ONE_DOT_FIVE, gwt.getVersion() );
    }

    public void testGwtVersion1462Detection()
    {
        File basedir = new File( System.getProperty( "basedir", "." ) );
        File gwtDevJar = new File( basedir, "target/test-classes/gwt-dev-1.4.62-fake.jar" );
        GwtRuntime gwt = new GwtRuntime( null, gwtDevJar );
        assertEquals( GwtVersion.ONE_DOT_FOUR, gwt.getVersion() );
    }

}
