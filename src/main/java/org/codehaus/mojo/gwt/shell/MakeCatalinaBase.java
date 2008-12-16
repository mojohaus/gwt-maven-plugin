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
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author cooper
 */
public class MakeCatalinaBase
{

    private File baseDir;

    private File sourceWebXml;

    private String shellServletMappingURL;

    /**
     * @param baseDir
     * @param sourceWebXml
     * @param shellServletMappingURL
     */
    public MakeCatalinaBase( File baseDir, File sourceWebXml, String shellServletMappingURL )
    {
        super();
        this.baseDir = baseDir;
        this.sourceWebXml = sourceWebXml;
        this.shellServletMappingURL = shellServletMappingURL;
    }

    public void setup()
        throws Exception
    {
        baseDir.mkdirs();

        File conf = new File( baseDir, "conf" );
        conf.mkdirs();

        File gwt = new File( conf, "gwt" );
        gwt.mkdirs();

        File localhost = new File( gwt, "localhost" );
        localhost.mkdirs();

        File webapps = new File( baseDir, "webapps" );
        webapps.mkdirs();

        File root = new File( webapps, "ROOT" );
        root.mkdirs();

        File webinf = new File( root, "WEB-INF" );
        webinf.mkdirs();
        new File( baseDir, "work" ).mkdirs();

        FileOutputStream fos = new FileOutputStream( new File( conf, "web.xml" ) );
        InputStream baseWebXml = getClass().getResourceAsStream( "baseWeb.xml" );
        if (baseWebXml != null)
        {
            IOUtils.copy( baseWebXml, fos );
        }
        File mergeWeb = new File( webinf, "web.xml" );
        if ( sourceWebXml.exists() )
        {
            GwtShellWebProcessor p =
                new GwtShellWebProcessor( mergeWeb.getAbsolutePath(), sourceWebXml, shellServletMappingURL );
            p.process();
        }
        else
        {
            fos = new FileOutputStream( mergeWeb );
            IOUtils.copy( getClass().getResourceAsStream( "emptyWeb.xml" ), fos );
        }

    }
}
