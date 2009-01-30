package org.codehaus.mojo.gwt;

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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;

/**
 * @author Robert Scholte
 */
public class GenerateAsyncMojoTest
    extends AbstractGwtMojoTestCase
{

    public void testGWT1()
        throws Exception
    {
        GenerateAsyncMojo mojo = (GenerateAsyncMojo) newMojo( "src/test/MGWT-1" );

        //required fields in mojo
        setVariableValueToObject( mojo, "servicePattern", "**\\/*Service.java" );
        setVariableValueToObject( mojo, "rpcPattern", "{0}.rpc" );
        File generateDirectory = new File( new File( getBasedir(), "target/test/MGWT-1/target" ),
                                           "/generated-sources/gwt" );
        setVariableValueToObject( mojo, "generateDirectory", generateDirectory );

        //run goal
        mojo.execute();

        //analyze generating java-class
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.addSource( new File( generateDirectory, "org/mycompany/NumberServiceAsync.java" ) );
        JavaClass jClass = builder.getClassByName( "org.mycompany.NumberServiceAsync" );
        JavaMethod jMethod = jClass.getMethods()[0];
        assertEquals( "getNumberList", jMethod.getName() );
        JavaParameter jParameter = jMethod.getParameterByName( "callback" );
        assertNotNull( jParameter );
        assertEquals( "com.google.gwt.user.client.rpc.AsyncCallback<java.util.List<java.lang.Number>>", jParameter
            .getType().getGenericValue() );
    }

    protected String getGoal()
    {
        return "generateAsync";
    }

}
