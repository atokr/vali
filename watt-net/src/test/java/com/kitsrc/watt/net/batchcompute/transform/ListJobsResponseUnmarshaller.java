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
package com.kitsrc.watt.net.batchcompute.transform;

//import com.aliyuncs.batchcompute.model.ListJobsResponse;
//import com.aliyuncs.transform.UnmarshallerContext;
import com.kitsrc.watt.net.batchcompute.model.ListJobsResponse;
import com.kitsrc.watt.net.transform.UnmarshallerContext;
import java.util.Map;

public class ListJobsResponseUnmarshaller {

    public static ListJobsResponse unmarshall(ListJobsResponse listJobsResponse, UnmarshallerContext context) {
        Map<String, String> headersMap = context.getHttpResponse().getSysHeaders();
        listJobsResponse.setRequestId(headersMap.get("Request-Id"));
        return listJobsResponse;
    }
}