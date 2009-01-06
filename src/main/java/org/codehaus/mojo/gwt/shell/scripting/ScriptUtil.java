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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.shell.PlatformUtil;
import org.codehaus.mojo.gwt.shell.scripting.TestResult.TestCode;

public final class ScriptUtil
{

    private ScriptUtil()
    {
    }

    public static TestResult runTestScript( final File exec )
        throws MojoExecutionException
    {
        TestResult testResult = new TestResult();
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        ProcessWatcher pw = null;
        if ( PlatformUtil.OS_NAME.startsWith( PlatformUtil.WINDOWS ) )
        {
            pw = new ProcessWatcher( "\"" + exec.getAbsolutePath() + "\"" );
        }
        else
        {
            pw = new ProcessWatcher( exec.getAbsolutePath().replaceAll( " ", "\\ " ) );
        }

        try
        {
            pw.startProcess( out, err );
            pw.waitFor();

            // if err has anything it's an exception - excepting special Leopard stuff
            if ( err.length() > 0 )
            {
                boolean validError = true;

                // the Mac VM will log CocoaComponent messages to stderr, falsely triggering the exception
                if ( PlatformUtil.OS_NAME.startsWith( PlatformUtil.MAC ) )
                {
                    validError = false;
                    final String[] errLines = err.toString().split( "\n" );
                    for ( int i = 0; i < errLines.length; ++i )
                    {
                        final String currentErrLine = errLines[i].trim();
                        if ( !currentErrLine.endsWith( "[JavaCocoaComponent compatibility mode]: Enabled" )
                            && !currentErrLine.endsWith( "[JavaCocoaComponent compatibility mode]: Setting timeout for SWT to 0.100000" )
                            && currentErrLine.length() != 0 )
                        {
                            validError = true;
                            break;
                        }
                    }
                }

                if ( validError )
                {
                    throw new MojoExecutionException( "error attempting to run test - " + exec.getName() + " - "
                        + err.toString() );
                }
            }

            // otherwise populate and return the TestResult
            //
            // SUCCESS ends up in system.out "OK"
            // FAILURE ends up in system.out "FAILURES!!!"
            // ERROR ends up in system.out "FAILURES!!!"
            //
            // example gwt output below
            //
            // FAILURES!!!
            // Tests run: 1, Failures: 0, Errors: 1
            //
            // OK (1 test)

            String[] lines = null;
            if ( PlatformUtil.OS_NAME.startsWith( PlatformUtil.WINDOWS ) )
            {
                lines = out.toString().split( "\r\n" );
            }
            else
            {
                lines = out.toString().split( "\n" );
            }
            String lastLine = lines[lines.length - 1];
            testResult.lastLine = lastLine;
            if ( lastLine.indexOf( "Tests run" ) != -1 )
            {
                // TODO add parsing to differentiate FAILURE and ERROR, or BOTH?
                testResult.code = TestCode.FAILURE;
            }
            else if ( lastLine.indexOf( "OK" ) != -1 )
            {
                testResult.code = TestCode.SUCCESS;
            }
            testResult.message = out.toString();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "error attempting to run test - " + exec.getName() + " - "
                + e.getMessage(), e );
        }
        return testResult;
    }
}
