package org.codehaus.mojo.gwt;


/**
 * @author ndeloof
 *
 */
public abstract class AbstractGwtModuleMojo
    extends AbstractGwtMojo
{

    /**
     * A single GWT module (Shortcut for modules)
     * 
     * @parameter
     */
    private String module;

    /**
     * The project GWT modules.
     * 
     * @parameter
     */
    private String[] modules;

    /**
     *
     */
    public AbstractGwtModuleMojo()
    {
        super();
    }

    /**
     * @param module the module to set
     */
    public void setModule( String module )
    {
        this.modules = new String[] { module };
    }

    /**
     * @return the modules
     */
    public String[] getModules()
    {
        return modules;
    }

}