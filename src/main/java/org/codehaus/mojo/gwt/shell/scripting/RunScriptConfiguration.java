package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;

/**
 * @author ndeloof
 *
 */
public interface RunScriptConfiguration
    extends GwtShellScriptConfiguration
{

    /**
     * @return
     */
    String getRunTarget();


    /**
     * @return
     */
    File getTomcat();

    /**
     * @return
     */
    int getPort();

}
