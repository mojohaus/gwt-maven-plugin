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
    String[] getCompileTarget();

    /**
     * @return
     */
    boolean isEnableAssertions();

}
