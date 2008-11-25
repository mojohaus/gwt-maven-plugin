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

import static org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo.LINUX;
import static org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo.MAC;
import static org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo.OS_NAME;
import static org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo.WINDOWS;

public class ArtifactNameUtil
{

    /**
     * Util for artifact and platform names stuff.
     *
     * @author ccollins
     */
    private ArtifactNameUtil()
    {
    }

    /**
     * Convenience return platform name.
     *
     * @return
     */
    public static final String getPlatformName()
    {
        String result = WINDOWS;
        if ( OS_NAME.startsWith( MAC ) )
        {
            result = MAC;
        }
        else if ( OS_NAME.startsWith( LINUX ) )
        {
            result = LINUX;
        }
        return result;
    }

    /**
     * Guess dev jar name based on platform.
     *
     * @return
     */
    public static final String guessDevJarName()
    {
        if ( OS_NAME.startsWith( WINDOWS ) )
        {
            return "gwt-dev-windows.jar";
        }
        else if ( OS_NAME.startsWith( MAC ) )
        {
            return "gwt-dev-mac.jar";
        }
        else
        {
            return "gwt-dev-linux.jar";
        }
    }
}