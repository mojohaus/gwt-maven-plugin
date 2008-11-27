package org.codehaus.mojo.gwt.shell;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;

/**
 * Extends the gwt goal and runs the project in the GWTShell with a debugger port hook (optionally suspended).
 *
 * @goal debug
 * @description Runs the project with a debugger port hook (optionally suspended).
 * @author cooper
 */
public class DebugMojo
    extends RunMojo
{

    /**
     * Port to listen for debugger connection on.
     * 
     * @parameter default-value="8000"
     */
    private int debugPort;

    /**
     * Whether or not to suspend execution until a debugger connects.
     *
     * @parameter default-value="true"
     */
    private boolean debugSuspend;

    @Override
    public void doExecute(GwtRuntime runtime)
        throws MojoExecutionException, MojoFailureException
    {
        if (isDebugSuspend())
            getLog().info("starting debugger on port " + getDebugPort() + " in suspend mode");
        else
            getLog().info("starting debugger on port " + getDebugPort());
        super.execute();
    }


    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getDebugPort()
     */
    public int getDebugPort()
    {
        return this.debugPort;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isDebugSuspend()
     */
    public boolean isDebugSuspend()
    {
        return this.debugSuspend;
    }
}
