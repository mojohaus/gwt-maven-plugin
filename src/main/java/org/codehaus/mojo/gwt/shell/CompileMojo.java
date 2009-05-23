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
 * Invokes the GWTCompiler for the project source.
 *
 * @goal compile
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
// @phase prepare-package should be even better to avoid unecessary gwt:compile when used with m2eclipse
public class CompileMojo
    extends AbstractCompileMojo
{

    /**
     * Location on filesystem where GWT will write output files (-out option to GWTCompiler).
     *
     * @parameter expression="${gwt.war}" default-value="${project.buildDirectory}/${project.finalName}"
     * @alias outputDirectory
     */
    private File output;

    public File getOutput()
    {
        return output;
    }

    
}
