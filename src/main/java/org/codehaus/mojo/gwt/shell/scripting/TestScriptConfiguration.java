package org.codehaus.mojo.gwt.shell.scripting;

/**
 * @author ndeloof
 *
 */
public interface TestScriptConfiguration
    extends RunScriptConfiguration
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
