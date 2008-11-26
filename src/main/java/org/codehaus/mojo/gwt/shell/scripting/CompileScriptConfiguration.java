package org.codehaus.mojo.gwt.shell.scripting;

/**
 * @author ndeloof
 *
 */
public interface CompileScriptConfiguration
    extends RunScriptConfiguration
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
