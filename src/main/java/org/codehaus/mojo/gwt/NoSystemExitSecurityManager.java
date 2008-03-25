package org.codehaus.mojo.gwt;

/*
 * Copyright 2006- org.codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import java.io.FileDescriptor;
import java.net.InetAddress;

/**
 * A custom SecurityManager that delegates to it's parent BUT checkExit.
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
class NoSystemExitSecurityManager
    extends SecurityManager
{
    /** The parent securityManager to delegate security checks (if not null) */
    protected SecurityManager parent;

    /**
     * {@inheritDoc}
     */
    public NoSystemExitSecurityManager( SecurityManager parent )
    {
        super();
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    public void checkExit( int status )
    {
        throw new SystemExitSecurityException( "Intercepted System.exit" );
    }

    /**
     * {@inheritDoc}
     */
    public void checkAccept( String host, int port )
    {
        if ( parent != null )
        {
            parent.checkAccept( host, port );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkAccess( Thread t )
    {
        if ( parent != null )
        {
            parent.checkAccess( t );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkAccess( ThreadGroup g )
    {
        if ( parent != null )
        {
            parent.checkAccess( g );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkAwtEventQueueAccess()
    {
        if ( parent != null )
        {
            parent.checkAwtEventQueueAccess();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkConnect( String host, int port, Object context )
    {
        if ( parent != null )
        {
            parent.checkConnect( host, port, context );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkConnect( String host, int port )
    {
        if ( parent != null )
        {
            parent.checkConnect( host, port );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkCreateClassLoader()
    {
        if ( parent != null )
        {
            parent.checkCreateClassLoader();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkDelete( String file )
    {
        if ( parent != null )
        {
            parent.checkDelete( file );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkExec( String cmd )
    {
        if ( parent != null )
        {
            parent.checkExec( cmd );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLink( String lib )
    {
        if ( parent != null )
        {
            parent.checkLink( lib );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkListen( int port )
    {
        if ( parent != null )
        {
            parent.checkListen( port );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkMemberAccess( Class arg0, int arg1 )
    {
        if ( parent != null )
        {
            parent.checkMemberAccess( arg0, arg1 );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkMulticast( InetAddress maddr, byte ttl )
    {
        if ( parent != null )
        {
            parent.checkMulticast( maddr, ttl );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkMulticast( InetAddress maddr )
    {
        if ( parent != null )
        {
            parent.checkMulticast( maddr );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPackageAccess( String pkg )
    {
        if ( parent != null )
        {
            parent.checkPackageAccess( pkg );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPackageDefinition( String pkg )
    {
        if ( parent != null )
        {
            parent.checkPackageDefinition( pkg );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission( java.security.Permission perm, Object context )
    {
        if ( parent != null )
        {
            parent.checkPermission( perm, context );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission( java.security.Permission perm )
    {
        if ( parent != null )
        {
            parent.checkPermission( perm );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPrintJobAccess()
    {
        if ( parent != null )
        {
            parent.checkPrintJobAccess();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPropertiesAccess()
    {
        if ( parent != null )
        {
            parent.checkPropertiesAccess();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPropertyAccess( String key )
    {
        if ( parent != null )
        {
            parent.checkPropertyAccess( key );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkRead( FileDescriptor fd )
    {
        if ( parent != null )
        {
            parent.checkRead( fd );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkRead( String file, Object context )
    {
        if ( parent != null )
        {
            parent.checkRead( file, context );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkRead( String file )
    {
        if ( parent != null )
        {
            parent.checkRead( file );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkSecurityAccess( String target )
    {
        if ( parent != null )
        {
            parent.checkSecurityAccess( target );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkSetFactory()
    {
        if ( parent != null )
        {
            parent.checkSetFactory();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkSystemClipboardAccess()
    {
        if ( parent != null )
        {
            parent.checkSystemClipboardAccess();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean checkTopLevelWindow( Object window )
    {
        if ( parent != null )
        {
            return parent.checkTopLevelWindow( window );
        }
        return super.checkTopLevelWindow( window );
    }

    /**
     * {@inheritDoc}
     */
    public void checkWrite( FileDescriptor fd )
    {
        if ( parent != null )
        {
            parent.checkWrite( fd );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkWrite( String file )
    {
        if ( parent != null )
        {
            parent.checkWrite( file );
        }
    }
}