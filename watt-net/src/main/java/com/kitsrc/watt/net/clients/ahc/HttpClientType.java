package com.kitsrc.watt.net.clients.ahc;

public enum HttpClientType {
    /**
     * define Compatiblen,HttpClient,okHttp,Custom
     */

    Compatible(CompatibleUrlConnClient.class),
    ApacheHttpClient(com.kitsrc.watt.net.clients.ApacheHttpClient.class),
    OkHttp(null),
    Custom(null),
    ;

    private Class<? extends IHttpClient> implClass;

    HttpClientType(Class<? extends IHttpClient> implClass) {
        this.implClass = implClass;
    }

    public Class<? extends IHttpClient> getImplClass() {
        return implClass;
    }
}
