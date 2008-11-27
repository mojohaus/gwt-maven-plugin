package org.codehaus.mojo.gwt.shell;

import java.io.File;


/**
 * @author ndeloof
 *
 */
public abstract class AbstractGwtWebMojo
    extends AbstractGwtShellMojo
{
    /**
     * Source web.xml deployment descriptor that is used for GWT shell and for deployment WAR to "merge" servlet
     * entries.
     * 
     * @parameter default-value="${basedir}/src/main/webapp/WEB-INF/web.xml"
     */
    private File webXml;

    /**
     * Specifies whether or not to add the module name as a prefix to the servlet path when merging web.xml. If you set
     * this to false the exact path from the GWT module will be used, nothing else will be prepended.
     * 
     * @parameter default-value="false"
     */
    private boolean webXmlServletPathAsIs;

    public File getWebXml()
    {
        return webXml;
    }

    public boolean isWebXmlServletPathAsIs()
    {
        return webXmlServletPathAsIs;
    }


}
