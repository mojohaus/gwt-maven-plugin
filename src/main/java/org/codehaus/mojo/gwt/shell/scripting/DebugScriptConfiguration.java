package org.codehaus.mojo.gwt.shell.scripting;

/**
 * @author ndeloof
 *
 */
public interface DebugScriptConfiguration
    extends RunScriptConfiguration
{

    /**
     * @return
     */
    int getDebugPort();

    /**
     * @return
     */
    boolean isDebugSuspend();

}
