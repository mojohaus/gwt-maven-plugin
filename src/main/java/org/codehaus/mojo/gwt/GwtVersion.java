package org.codehaus.mojo.gwt;

/**
 * @author ndeloof
 */
public enum GwtVersion
{
    ONE_DOT_FOUR
    {
        @Override
        public String getShellFQCN()
        {
            return "com.google.gwt.dev.GWTShell";
        }
    },
    ONE_DOT_FIVE
    {
        @Override
        public String getShellFQCN()
        {
            return "com.google.gwt.dev.GWTShell";
        }
    },
    ONE_DOT_SIX
    {
        @Override
        public String getShellFQCN()
        {
            return "com.google.gwt.dev.HostedMode";
        }

        @Override
        public boolean fixEmbeddedTomcatClassloaderIssue()
        {
            return true;
        }
    },
    FUTURE
    {
        @Override
        public String getShellFQCN()
        {
            return "com.google.gwt.dev.HostedMode";
        }

        @Override
        public boolean fixEmbeddedTomcatClassloaderIssue()
        {
            return true;
        }
    };

    /**
     * @return fully qualified class name of the GWTShell "main" class
     */
    public abstract String getShellFQCN();

    /**
     * @return <code>true</code> if this version fixes EmbeddedTomcatServer issue with SystemClassLoader
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=1032
     */
    public boolean fixEmbeddedTomcatClassloaderIssue()
    {
        return false;
    }

    static GwtVersion fromMavenVersion( String version )
    {
        if ( version.startsWith( "1.4" ) )
        {
            return ONE_DOT_FOUR;
        }
        if ( version.startsWith( "1.5" ) )
        {
            return ONE_DOT_FIVE;
        }
        if ( version.startsWith( "1.6" ) )
        {
            return ONE_DOT_SIX;
        }
        return FUTURE;
    }
}
