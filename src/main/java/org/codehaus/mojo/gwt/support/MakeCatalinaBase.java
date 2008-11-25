package org.codehaus.mojo.gwt.support;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 *
 * @author cooper
 */
public class MakeCatalinaBase {
    /**
     * default read size for stream copy
     */
    static final int DEFAULT_BUFFER_SIZE = 1024;

    /** Copies the data from an InputStream object to an OutputStream object.
     * @param sourceStream The input stream to be read.
     * @param destinationStream The output stream to be written to.
     * @return int value of the number of bytes copied.
     * @exception IOException from java.io calls.
     */
    static int copyStream(
            InputStream sourceStream, OutputStream destinationStream
            ) throws IOException {
        int bytesRead = 0;
        int totalBytes = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        while(bytesRead >= 0) {
            bytesRead = sourceStream.read(buffer, 0, buffer.length);

            if(bytesRead > 0) {
                destinationStream.write(buffer, 0, bytesRead);
            }

            totalBytes += bytesRead;
        }

        destinationStream.flush();
        destinationStream.close();
        return totalBytes;
    }

    public static void main(String[] args) throws Exception {
        String baseDir = args[0];
        String sourceWebXml = args[1];
        String shellServletMappingURL = args[2];

        File catalinaBase = new File(baseDir);
        catalinaBase.mkdirs();

        File conf = new File(catalinaBase, "conf");
        conf.mkdirs();

        File gwt = new File(conf, "gwt");
        gwt.mkdirs();

        File localhost = new File(gwt, "localhost");
        localhost.mkdirs();

        File webapps = new File(catalinaBase, "webapps");
        webapps.mkdirs();

        File root = new File(webapps, "ROOT");
        root.mkdirs();

        File webinf = new File(root, "WEB-INF");
        webinf.mkdirs();
        new File(catalinaBase, "work").mkdirs();

        FileOutputStream fos = new FileOutputStream( new File( conf, "web.xml") );
        MakeCatalinaBase.copyStream( MakeCatalinaBase.class.getResourceAsStream("baseWeb.xml"),
                fos);
        File mergeWeb = new File( webinf, "web.xml");
        File sourceWeb = new File( sourceWebXml );
        if( sourceWeb.exists() ){
            GwtShellWebProcessor p = new GwtShellWebProcessor( mergeWeb.getAbsolutePath(), sourceWebXml, shellServletMappingURL );
            p.process();
        } else {
            fos = new FileOutputStream( mergeWeb );
            MakeCatalinaBase.copyStream( MakeCatalinaBase.class.getResourceAsStream("emptyWeb.xml"),
                    fos);
        }

    }
}
