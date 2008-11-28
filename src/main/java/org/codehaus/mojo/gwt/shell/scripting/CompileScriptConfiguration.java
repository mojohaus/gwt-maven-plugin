package org.codehaus.mojo.gwt.shell.scripting;

/**
 * @author ndeloof
 *
 */
public interface CompileScriptConfiguration
    extends GwtShellScriptConfiguration
{

    /**
     * @return
     */
    String[] getModules();

    /**
     * @return
     */
    boolean isEnableAssertions();

}
