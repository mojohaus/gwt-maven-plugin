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
 */
class NoSystemExitSecurityManager
    extends SecurityManager
{
    protected SecurityManager parent;

    public NoSystemExitSecurityManager( SecurityManager parent )
    {
        super();
        this.parent = parent;
    }

    public void checkExit( int status )
    {
        throw new SystemExitSecurityException( "Intercepted System.exit" );
    }

    public void checkAccept( String host, int port )
    {
        if ( parent != null )
        {
            parent.checkAccept( host, port );
        }
    }

    public void checkAccess( Thread t )
    {
        if ( parent != null )
        {
            parent.checkAccess( t );
        }
    }

    public void checkAccess( ThreadGroup g )
    {
        if ( parent != null )
        {
            parent.checkAccess( g );
        }
    }

    public void checkAwtEventQueueAccess()
    {
        if ( parent != null )
        {
            parent.checkAwtEventQueueAccess();
        }
    }

    public void checkConnect( String host, int port, Object context )
    {
        if ( parent != null )
        {
            parent.checkConnect( host, port, context );
        }
    }

    public void checkConnect( String host, int port )
    {
        if ( parent != null )
        {
            parent.checkConnect( host, port );
        }
    }

    public void checkCreateClassLoader()
    {
        if ( parent != null )
        {
            parent.checkCreateClassLoader();
        }
    }

    public void checkDelete( String file )
    {
        if ( parent != null )
        {
            parent.checkDelete( file );
        }
    }

    public void checkExec( String cmd )
    {
        if ( parent != null )
        {
            parent.checkExec( cmd );
        }
    }

    public void checkLink( String lib )
    {
        if ( parent != null )
        {
            parent.checkLink( lib );
        }
    }

    public void checkListen( int port )
    {
        if ( parent != null )
        {
            parent.checkListen( port );
        }
    }

    public void checkMemberAccess( Class arg0, int arg1 )
    {
        if ( parent != null )
        {
            parent.checkMemberAccess( arg0, arg1 );
        }
    }

    public void checkMulticast( InetAddress maddr, byte ttl )
    {
        if ( parent != null )
        {
            parent.checkMulticast( maddr, ttl );
        }
    }

    public void checkMulticast( InetAddress maddr )
    {
        if ( parent != null )
        {
            parent.checkMulticast( maddr );
        }
    }

    public void checkPackageAccess( String pkg )
    {
        if ( parent != null )
        {
            parent.checkPackageAccess( pkg );
        }
    }

    public void checkPackageDefinition( String pkg )
    {
        if ( parent != null )
        {
            parent.checkPackageDefinition( pkg );
        }
    }

    public void checkPermission( java.security.Permission perm, Object context )
    {
        if ( parent != null )
        {
            parent.checkPermission( perm, context );
        }
    }

    public void checkPermission( java.security.Permission perm )
    {
        if ( parent != null )
        {
            parent.checkPermission( perm );
        }
    }

    public void checkPrintJobAccess()
    {
        if ( parent != null )
        {
            parent.checkPrintJobAccess();
        }
    }

    public void checkPropertiesAccess()
    {
        if ( parent != null )
        {
            parent.checkPropertiesAccess();
        }
    }

    public void checkPropertyAccess( String key )
    {
        if ( parent != null )
        {
            parent.checkPropertyAccess( key );
        }
    }

    public void checkRead( FileDescriptor fd )
    {
        if ( parent != null )
        {
            parent.checkRead( fd );
        }
    }

    public void checkRead( String file, Object context )
    {
        if ( parent != null )
        {
            parent.checkRead( file, context );
        }
    }

    public void checkRead( String file )
    {
        if ( parent != null )
        {
            parent.checkRead( file );
        }
    }

    public void checkSecurityAccess( String target )
    {
        if ( parent != null )
        {
            parent.checkSecurityAccess( target );
        }
    }

    public void checkSetFactory()
    {
        if ( parent != null )
        {
            parent.checkSetFactory();
        }
    }

    public void checkSystemClipboardAccess()
    {
        if ( parent != null )
        {
            parent.checkSystemClipboardAccess();
        }
    }

    public boolean checkTopLevelWindow( Object window )
    {
        if ( parent != null )
        {
            return parent.checkTopLevelWindow( window );
        }
        return super.checkTopLevelWindow( window );
    }

    public void checkWrite( FileDescriptor fd )
    {
        if ( parent != null )
        {
            parent.checkWrite( fd );
        }
    }

    public void checkWrite( String file )
    {
        if ( parent != null )
        {
            parent.checkWrite( file );
        }
    }
}