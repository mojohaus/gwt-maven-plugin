package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;

/**
 * @author ndeloof
 *
 */
public interface I18nScriptConfiguration
    extends RunScriptConfiguration
{

    /**
     * @return
     */
    String[] getI18nConstantsNames();

    /**
     * @return
     */
    File getI18nOutputDir();

    /**
     * @return
     */
    String[] getI18nMessagesNames();

}
