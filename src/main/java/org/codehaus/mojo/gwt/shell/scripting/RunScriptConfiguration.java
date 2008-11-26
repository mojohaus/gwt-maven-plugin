package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;

/**
 * @author ndeloof
 *
 */
public interface RunScriptConfiguration
    extends MavenScriptConfiguration
{

    /**
     * @return
     */
    String getExtraJvmArgs();

    /**
     * @return
     */
    File getTomcat();

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
    int getPort();

    /**
     * @return
     */
    boolean isNoServer();

    /**
     * @return
     */
    String getRunTarget();

    /**
     * @return
     */
    File getGwtHome();

    /**
     * @return
     */
    boolean getSourcesOnPath();

    /**
     * @return
     */
    boolean getResourcesOnPath();

    /**
     * @return
     */
    String getGwtVersion();


}