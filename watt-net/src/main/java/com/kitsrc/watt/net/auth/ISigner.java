package com.kitsrc.watt.net.auth;

import java.security.InvalidKeyException;

@Deprecated
public interface ISigner {

    String getSignerName();

    String getSignerVersion();

    String signString(String source, String accessSecret) throws InvalidKeyException, IllegalStateException;

}
