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
package com.kitsrc.watt.net.v20150814;

//import com.aliyuncs.RoaAcsRequest;
//import com.aliyuncs.http.MethodType;

/**
 * @author auto create
 */
public class DeleteDBSourceRequest extends RoaAcsRequest<DeleteDBSourceResponse> {

    private String projectName;
    private String sourceName;

    public DeleteDBSourceRequest() {
        super("Cms", "2015-08-14", "DeleteDBSource");
        setUriPattern("/projects/[ProjectName]/sources/[SourceName]");
        setMethod(MethodType.DELETE);
    }

    public String getProjectName() {
        return this.projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        putPathParameter("ProjectName", projectName);
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
        putPathParameter("SourceName", sourceName);
    }

    @Override
    public Class<DeleteDBSourceResponse> getResponseClass() {
        return DeleteDBSourceResponse.class;
    }

}
