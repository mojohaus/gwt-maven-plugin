package org.codehaus.mojo.gwt.shell;

import org.codehaus.plexus.util.Os;

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


/**
 *
 * @version $Id$
 */
public class ArtifactNameUtil
{
    public static final String WINDOWS = "windows";

    public static final String LINUX = "linux";

    public static final String MAC = "mac";

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
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return WINDOWS;
        }
        if ( Os.isFamily( Os.FAMILY_MAC ) )
        {
            return MAC;
        }
        return LINUX;
    }

    /**
     * Guess dev jar name based on platform.
     *
     * @return
     */
    public static final String guessDevJarName()
    {
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return "gwt-dev-windows.jar";
        }
        else if ( Os.isFamily( Os.FAMILY_MAC ) )
        {
            return "gwt-dev-mac.jar";
        }
        else
        {
            return "gwt-dev-linux.jar";
        }
    }
}