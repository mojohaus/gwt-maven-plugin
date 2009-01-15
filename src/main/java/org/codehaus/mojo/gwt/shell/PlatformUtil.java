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

import java.io.File;
import java.util.Locale;

import org.codehaus.plexus.util.FileUtils;

/**
 * @author ndeloof
 */
public final class PlatformUtil
{

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String WINDOWS = "windows";

    public static final String LINUX = "linux";

    public static final String MAC = "mac";

    public static final String LEOPARD = "leopard";

    public static final String JAVA_COMMAND =
    ( System.getProperty( "java.home" ) != null ) ? FileUtils.normalize( System.getProperty( "java.home" )
        + File.separator + "bin" + File.separator + "java" ) : "java";

    /**
     * Utility class
     */
    private PlatformUtil()
    {
        super();
    }

    /**
     * @return true if running on Windows
     */
    public static boolean onWindows()
    {
        return OS_NAME.startsWith( WINDOWS );
    }

    /**
     * @return true if running on a Mac
     */
    public static boolean onMac()
    {
        return OS_NAME.startsWith( MAC );
    }
}
