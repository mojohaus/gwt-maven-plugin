package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;

/**
 * @author ndeloof
 *
 */
public interface GwtShellScriptConfiguration
    extends MavenScriptConfiguration
{

    /**
     * @return
     */
    String getExtraJvmArgs();

    /**
     * @return
     */
    File getGen();

    /**
     * @return
     */
    String getLogLevel();

    /**
     * @return
     */
    String getStyle();

    /**
     * @return
     */
    File getOutput();

    /**
     * @return
     */
    boolean isNoServer();

    /**
     * @return
     */
    boolean getSourcesOnPath();

    /**
     * @return
     */
    boolean getResourcesOnPath();

}
