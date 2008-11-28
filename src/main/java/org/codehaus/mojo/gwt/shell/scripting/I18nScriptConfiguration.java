package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;

/**
 * @author ndeloof
 *
 */
public interface I18nScriptConfiguration
    extends GwtShellScriptConfiguration
{

    public String[] getI18nMessagesBundles();

    public String[] getI18nConstantsBundles();

    public String[] getI18nConstantsWithLookupBundles();

    public File getGenerateDirectory();

}
