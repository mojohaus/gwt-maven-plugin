package org.codehaus.mojo.gwt.shell.scripting;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class executes a commandline and watches it's output.
 *
 * @author willpugh
 */
public class ProcessWatcher {
   public static final long DEFAULT_SLEEP = 200;

   Process process;
   Object command;
   String[] envirionment;
   File dir;
   StreamSucker out;
   StreamSucker err;

   private ProcessWatcher( Object command, String[] envirionment, File dir )
    {
        this.command = command;
        this.envirionment = envirionment;
        this.dir = dir;
    }

    public ProcessWatcher( String command, String[] envirionment, File dir )
    {
        this( (Object) command, envirionment, dir );
    }

    public ProcessWatcher( String[] command, String[] envirionment, File dir )
    {
        this( (Object) command, envirionment, dir );
    }

    public ProcessWatcher( String command, String[] envirionment )
    {
        this( (Object) command, envirionment, null );
    }

    public ProcessWatcher( String[] command, String[] envirionment )
    {
        this( (Object) command, envirionment, null );
    }

    public ProcessWatcher( String command )
    {
        this( (Object) command, null, null );
    }

    public ProcessWatcher( String[] command )
    {
        this( (Object) command, null, null );
    }

    public void startProcess()
        throws IOException
    {

        //First start the process
        if ( command instanceof String[] )
        {
            process = Runtime.getRuntime().exec( (String[]) command, envirionment, dir );
        }
        else
        {
            process = Runtime.getRuntime().exec( (String) command, envirionment, dir );
        }

        //Now start the suckers
        if ( out == null )
        {
            out = new StreamSucker( new NulStream() );
        }

        if ( err == null )
        {
            err = new StreamSucker( new NulStream() );
        }

        out.setIn( process.getInputStream() );
        err.setIn( process.getErrorStream() );

        out.start();
        err.start();
    }

    public void startProcess( OutputStream stdout, OutputStream stderr )
        throws IOException
    {
        if ( stdout != null )
            out = new StreamSucker( stdout );

        if ( stderr != null )
            err = new StreamSucker( stderr );

        startProcess();
    }

    public void startProcess( StringBuffer stdout, StringBuffer stderr )
        throws IOException
    {
        if ( stdout != null )
            out = new StreamSucker( new StringBufferStream( stdout ) );

        if ( stderr != null )
            err = new StreamSucker( new StringBufferStream( stderr ) );

        startProcess();
    }

    public void startProcess( StringBuilder stdout, StringBuilder stderr )
        throws IOException
    {
        if ( stdout != null )
            out = new StreamSucker( new StringBuilderStream( stdout ) );

        if ( stderr != null )
            err = new StreamSucker( new StringBuilderStream( stderr ) );

        startProcess();
    }

    public OutputStream getStdIn()
    {
        return process.getOutputStream();
    }

    public int exitValue()
    {
        return process.exitValue();
    }

    public void destroy()
    {
        process.destroy();
    }

    public int waitFor()
        throws InterruptedException
    {
        try
        {
            process.waitFor();
        }
        finally
        {
            out.shutdown();
            err.shutdown();
        }

        out.join();
        err.join();

        return process.exitValue();
    }

    static public class StreamSucker
        extends Thread
    {

        private final long sleeptime;

        private final OutputStream out;

        private InputStream in;

        volatile boolean allDone = false;

        public StreamSucker( OutputStream out, long sleeptime )
        {
            this.sleeptime = sleeptime;
            if ( out == null )
                this.out = new NulStream();
            else
                this.out = out;
        }

        public StreamSucker( OutputStream out )
        {
            this( out, DEFAULT_SLEEP );
        }

        public StreamSucker()
        {
            this( null, DEFAULT_SLEEP );
        }

        public void shutdown()
        {
            allDone = true;
        }

        public void siphonAvailableBytes( byte[] buf )
            throws IOException
        {
            int available = getIn().available();
            while ( available > 0 )
            {
                available = getIn().read( buf );
                getOut().write( buf, 0, available );
                available = getIn().available();
            }
        }

        public void run()
        {
            byte[] buf = new byte[4096];

            try
            {

                while ( !allDone )
                {
                    synchronized ( this )
                    {
                        this.wait( getSleeptime() );
                    }
                    siphonAvailableBytes( buf );
                }

                //One last siphoning to make sure we got everything
                siphonAvailableBytes( buf );

            }
            catch ( InterruptedException e )
            {
                //We got interupted, time to go. . .
            }
            catch ( IOException e )
            {

            }
            finally
            {
                try
                {
                    out.flush();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }

        }

        public long getSleeptime()
        {
            return sleeptime;
        }

        public OutputStream getOut()
        {
            return out;
        }

        public InputStream getIn()
        {
            return in;
        }

        public void setIn( InputStream in )
        {
            this.in = in;
        }
    }

    static public class NulStream
        extends OutputStream
    {
        public void write( int i )
            throws IOException
        {
            //Null Op
        }
    }

    static public class StringBufferStream
        extends OutputStream
    {
        final StringBuffer buf;

        public StringBufferStream( StringBuffer buf )
        {
            this.buf = buf;
        }

        public void write( int i )
            throws IOException
        {
            buf.append( (char) i );
        }
    }

    static public class StringBuilderStream
        extends OutputStream
    {
        final StringBuilder buf;

        public StringBuilderStream( StringBuilder buf )
        {
            this.buf = buf;
        }

        public void write( int i )
            throws IOException
        {
            buf.append( (char) i );
        }
    }

}
