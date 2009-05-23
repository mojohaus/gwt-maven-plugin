/*
 * CompileMojo.java
 *
 * Created on January 13, 2007, 11:42 AM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 */
package org.codehaus.mojo.gwt.shell;

import java.io.File;

/**
 * Invokes the GWTCompiler for the project source. Used to match the war:inplace goal and compile the GWT application in
 * web application source folder for light development process. Requires to configure the SCM to exclude the generated
 * directory from src/main/webapp
 *
 * @goal inplace
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
// @phase prepare-package should be even better to avoid unecessary gwt:compile when used with m2eclipse
public class InPlaceMojo
    extends AbstractCompileMojo
{

    /**
     * Location of the web application static resources (same as maven-war-plugin parameter)
     *
     * @parameter default-value="${basedir}/src/main/webapp"
     */
    private File warSourceDirectory;

    public File getOutput()
    {
        return warSourceDirectory;
    }

    
}
