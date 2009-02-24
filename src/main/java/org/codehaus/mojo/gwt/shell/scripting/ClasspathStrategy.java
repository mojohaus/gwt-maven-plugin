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

public enum ClasspathStrategy
{
    /**
     * Use the {@link org.codehaus.mojo.gwt.fork.ForkBooter} class to setup a nested URLClassLoader based on dependency
     * listed in a file
     */
    FORKBOOTER,

    /**
     * Use an (empty) Jar with Main-Class and Class-Path entries in MANIFEST
     */
    JARBOOTER,

    /**
     * Use command line classpath option - known to be buggy on Windows due to command line lenght limitation
     */
    CLASSPATH_VARIABLE
}