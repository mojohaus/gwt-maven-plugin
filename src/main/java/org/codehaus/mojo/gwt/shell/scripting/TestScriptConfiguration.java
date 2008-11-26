package org.codehaus.mojo.gwt.shell.scripting;

/**
 * @author ndeloof
 *
 */
public interface TestScriptConfiguration
    extends GwtShellScriptConfiguration
{

    /**
     * @return
     */
    String getExtraTestArgs();

    /**
     * @return
     */
    String getTestFilter();

}
