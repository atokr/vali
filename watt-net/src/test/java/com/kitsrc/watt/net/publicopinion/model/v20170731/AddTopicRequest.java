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
package com.kitsrc.watt.net.publicopinion.model.v20170731;

//import com.aliyuncs.RpcAcsRequest;
//import com.aliyuncs.http.MethodType;

/**
 * @author auto create
 */
public class AddTopicRequest extends RpcAcsRequest<AddTopicResponse> {

    private String desc;
    private String name;

    public AddTopicRequest() {
        super("publicopinion", "2017-07-31", "AddTopic");
        setMethod(MethodType.POST);
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
        if (desc != null) {
            putBodyParameter("desc", desc);
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        if (name != null) {
            putBodyParameter("name", name);
        }
    }

    @Override
    public Class<AddTopicResponse> getResponseClass() {
        return AddTopicResponse.class;
    }

}
