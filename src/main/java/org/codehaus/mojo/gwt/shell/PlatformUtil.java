package org.codehaus.mojo.gwt.shell;

import java.io.File;
import java.util.Locale;

import org.codehaus.plexus.util.FileUtils;

/**
 * @author ndeloof
 */
public final class PlatformUtil
{

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String WINDOWS = "windows";

    public static final String LINUX = "linux";

    public static final String MAC = "mac";

    public static final String LEOPARD = "leopard";

    public static final String JAVA_COMMAND =
    ( System.getProperty( "java.home" ) != null ) ? FileUtils.normalize( System.getProperty( "java.home" )
        + File.separator + "bin" + File.separator + "java" ) : "java";

    /**
     * Utility class
     */
    private PlatformUtil()
    {
        super();
    }

    /**
     * @return true if running on Windows
     */
    public static boolean onWindows()
    {
        return OS_NAME.startsWith( WINDOWS );
    }

    /**
     * @return true if running on a Mac
     */
    public static boolean onMac()
    {
        return OS_NAME.startsWith( MAC );
    }
}
