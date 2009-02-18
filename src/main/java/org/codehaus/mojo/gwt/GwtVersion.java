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

        @Override
        public String getCompilerFQCN()
        {
            return "com.google.gwt.dev.GWTCompiler";
        }

    },
    ONE_DOT_FIVE
    {
        @Override
        public String getShellFQCN()
        {
            return "com.google.gwt.dev.GWTShell";
        }

        @Override
        public String getCompilerFQCN()
        {
            return "com.google.gwt.dev.GWTCompiler";
        }

    },
    ONE_DOT_SIX
    {

    },
    FUTURE
    {

    };

    /**
     * @return fully qualified class name of the GWTShell "main" class
     */
    public String getShellFQCN()
    {
        return "com.google.gwt.dev.HostedMode";
    }

    public String getCompilerFQCN()
    {
        return "com.google.gwt.dev.Compiler";
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
