package com.kitsrc.watt.net.clients.ahc;

public enum MethodType {
    /**
     * GET
     * PUT
     * POST
     * DELETE
     */
    GET(false),
    PUT(true),
    POST(true),
    DELETE(false),
    HEAD(false),
    OPTIONS(false);

    private boolean hasContent;

    MethodType(boolean hasContent) {
        this.hasContent = hasContent;
    }

    public boolean hasContent() {
        return hasContent;
    }
}