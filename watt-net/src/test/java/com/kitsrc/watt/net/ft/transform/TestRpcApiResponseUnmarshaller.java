/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.kitsrc.watt.net.ft.transform;

//import com.aliyuncs.ft.model.TestRpcApiResponse;
//import com.aliyuncs.ft.model.TestRpcApiResponse.Params;
//import com.aliyuncs.transform.UnmarshallerContext;


import com.kitsrc.watt.net.ft.model.TestRpcApiResponse;
import com.kitsrc.watt.net.transform.UnmarshallerContext;

public class TestRpcApiResponseUnmarshaller {

    public static TestRpcApiResponse unmarshall(TestRpcApiResponse testRpcApiResponse, UnmarshallerContext context) {

        testRpcApiResponse.setRemoteAddr(context.stringValue("TestRpcApiResponse.RemoteAddr"));
        testRpcApiResponse.setQueryString(context.stringValue("TestRpcApiResponse.QueryString"));
        testRpcApiResponse.setRequestURL(context.stringValue("TestRpcApiResponse.RequestURL"));

        TestRpcApiResponse.Params params = new TestRpcApiResponse.Params();
        params.setQueryParam(context.stringValue("TestRpcApiResponse.Params.QueryParam"));
        params.setBodyParam(context.stringValue("TestRpcApiResponse.Params.BodyParam"));
        testRpcApiResponse.setParams(params);

        return testRpcApiResponse;
    }
}