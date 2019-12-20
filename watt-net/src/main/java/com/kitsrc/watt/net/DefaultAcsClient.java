package com.kitsrc.watt.net;

import com.kitsrc.watt.net.auth.*;
import com.kitsrc.watt.net.endpoint.DefaultEndpointResolver;
import com.kitsrc.watt.net.endpoint.EndpointResolver;
import com.kitsrc.watt.net.endpoint.ResolveEndpointRequest;
import com.kitsrc.watt.net.exceptions.ClientException;
import com.kitsrc.watt.net.exceptions.ErrorCodeConstant;
import com.kitsrc.watt.net.exceptions.ErrorMessageConstant;
import com.kitsrc.watt.net.exceptions.ServerException;
import com.kitsrc.watt.net.http.*;
import com.kitsrc.watt.net.http.X509TrustAll;
import com.kitsrc.watt.net.profile.DefaultProfile;
import com.kitsrc.watt.net.profile.IClientProfile;
import com.kitsrc.watt.net.reader.Reader;
import com.kitsrc.watt.net.reader.ReaderFactory;
import com.kitsrc.watt.net.regions.ProductDomain;
import com.kitsrc.watt.net.transform.UnmarshallerContext;
import com.kitsrc.watt.net.unmarshaller.Unmarshaller;
import com.kitsrc.watt.net.unmarshaller.UnmarshallerFactory;
import com.kitsrc.watt.net.utils.IOUtils;
import com.kitsrc.watt.net.utils.LogUtils;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;

public class DefaultAcsClient implements IAcsClient {

    /*
        Now maxRetryNumber and autoRetry has no effect.
     */
    private int maxRetryNumber = 3;
    private boolean autoRetry = true;
    private IClientProfile clientProfile = null;
    private CredentialsProvider credentialsProvider;
    private DefaultCredentialsProvider defaultCredentialsProvider;

    private IHttpClient httpClient;
    private EndpointResolver endpointResolver;
    private static final String SIGNATURE_BEGIN = "string to sign is:";
    private final UserAgentConfig userAgentConfig = new UserAgentConfig();



    public DefaultAcsClient(String regionId) throws ClientException {
        this.clientProfile = DefaultProfile.getProfile(regionId);
        this.httpClient = HttpClientFactory.buildClient(this.clientProfile);
        this.defaultCredentialsProvider = new DefaultCredentialsProvider();
        this.endpointResolver = new DefaultEndpointResolver(this);
        this.appendUserAgent("HTTPClient", this.httpClient.getClass().getSimpleName());
    }

    public DefaultAcsClient(IClientProfile profile) {
        this(profile, new StaticCredentialsProvider(profile));
    }

    public DefaultAcsClient(IClientProfile profile, Credentials credentials) {
        this(profile, new StaticCredentialsProvider(credentials));
    }

    public DefaultAcsClient(IClientProfile profile, CredentialsProvider credentialsProvider) {
        this.clientProfile = profile;
        this.credentialsProvider = credentialsProvider;
        this.clientProfile.setCredentialsProvider(this.credentialsProvider);
        this.httpClient = HttpClientFactory.buildClient(this.clientProfile);
        this.endpointResolver = new DefaultEndpointResolver(this, profile);
        this.appendUserAgent("HTTPClient", this.httpClient.getClass().getSimpleName());
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request)
            throws ClientException, ServerException {
        return this.doAction(request, autoRetry, maxRetryNumber, this.clientProfile);
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts)
            throws ClientException, ServerException {
        return this.doAction(request, autoRetry, maxRetryCounts, this.clientProfile);
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, IClientProfile profile)
            throws ClientException, ServerException {
        return this.doAction(request, this.autoRetry, this.maxRetryNumber, profile);
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, String regionId, Credential credential)
            throws ClientException, ServerException {
        Signer signer = Signer.getSigner(new LegacyCredentials(credential));
        FormatType format = null;
        if (null == request.getSysRegionId()) {
            request.setSysRegionId(regionId);
        }
        return doAction(request, autoRetry, maxRetryNumber, regionId, new LegacyCredentials(credential), signer,
                format);
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request) throws ServerException, ClientException {
        HttpResponse baseResponse = this.doAction(request);
        return parseAcsResponse(request, baseResponse);
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts)
            throws ServerException, ClientException {
        HttpResponse baseResponse = this.doAction(request, autoRetry, maxRetryCounts);
        return parseAcsResponse(request, baseResponse);
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, IClientProfile profile)
            throws ServerException, ClientException {
        HttpResponse baseResponse = this.doAction(request, profile);
        return parseAcsResponse(request, baseResponse);
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, String regionId, Credential credential)
            throws ServerException, ClientException {
        HttpResponse baseResponse = this.doAction(request, regionId, credential);
        return parseAcsResponse(request, baseResponse);
    }

    @Override
    public <T extends AcsResponse> T getAcsResponse(AcsRequest<T> request, String regionId)
            throws ServerException, ClientException {
        if (null == request.getSysRegionId()) {
            request.setSysRegionId(regionId);
        }
        HttpResponse baseResponse = this.doAction(request);
        return parseAcsResponse(request, baseResponse);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommonResponse getCommonResponse(CommonRequest request) throws ServerException, ClientException {
        HttpResponse baseResponse = this.doAction(request.buildRequest());
        if (baseResponse.isSuccess()) {
            String stringContent = baseResponse.getHttpContentString();
            CommonResponse response = new CommonResponse();
            response.setData(stringContent);
            response.setHttpStatus(baseResponse.getStatus());
            response.setHttpResponse(baseResponse);
            return response;
        } else {
            FormatType format = baseResponse.getHttpContentType();
            AcsError error = readError(baseResponse, format);
            if (500 <= baseResponse.getStatus()) {
                throw new ServerException(error.getErrorCode(), error.getErrorMessage(), error.getRequestId());
            } else {
                throw new ClientException(error.getErrorCode(), error.getErrorMessage(), error.getRequestId());
            }
        }
    }

    @Override
    public <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryCounts,
            IClientProfile profile) throws ClientException, ServerException {
        if (null == profile) {
            throw new ClientException("SDK.InvalidProfile", "No active profile found.");
        }
        boolean retry = autoRetry;
        int retryNumber = maxRetryCounts;
        String region = profile.getRegionId();
        if (null == request.getSysRegionId()) {
            request.setSysRegionId(region);
        }
        Credentials credentials;
        if (null == credentialsProvider) {
            credentials = this.defaultCredentialsProvider.getCredentials();
        } else {
            credentials = this.credentialsProvider.getCredentials();
        }
        Signer signer = Signer.getSigner(credentials);
        FormatType format = profile.getFormat();

        return this.doAction(request, retry, retryNumber, request.getSysRegionId(), credentials, signer, format);
    }

    private <T extends AcsResponse> T parseAcsResponse(AcsRequest<T> request, HttpResponse baseResponse)
            throws ServerException, ClientException {
        FormatType format = baseResponse.getHttpContentType();
        if (baseResponse.isSuccess()) {
            return readResponse(request.getResponseClass(), baseResponse, format);
        } else {
            AcsError error = readError(baseResponse, format);
            if (500 <= baseResponse.getStatus()) {
                throw new ServerException(error.getErrorCode(), error.getErrorMessage(), error.getRequestId());
            } else if (400 == baseResponse.getStatus() && ("IncompleteSignature".equals(error.getErrorCode())
                    || "SignatureDoesNotMatch".equals(error.getErrorCode()))) {
                String errorMessage = error.getErrorMessage();
                Pattern startPattern = Pattern.compile(SIGNATURE_BEGIN);
                Matcher startMatcher = startPattern.matcher(errorMessage);
                if (startMatcher.find()) {
                    int start = startMatcher.end();
                    String errorStrToSign = errorMessage.substring(start);
                    if (request.strToSign.equals(errorStrToSign)) {
                        throw new ClientException("SDK.InvalidAccessKeySecret",
                                "Specified Access Key Secret is not valid.", error.getRequestId());
                    }
                }
            }
            throw new ClientException(error.getErrorCode(), error.getErrorMessage(), error.getRequestId());
        }
    }



    public ProductDomain getDomain(AcsRequest request, String regionId)
            throws ClientException {
        ProductDomain domain = null;
        if (request.getSysProductDomain() != null) {
            domain = request.getSysProductDomain();
        } else {
            ResolveEndpointRequest resolveEndpointRequest = new ResolveEndpointRequest(regionId,
                    request.getSysProduct(), request.getSysLocationProduct(), request.getSysEndpointType());
            resolveEndpointRequest.setProductEndpointMap(request.productEndpointMap);
            resolveEndpointRequest.setProductEndpointRegional(request.productEndpointRegional);
            resolveEndpointRequest.setProductNetwork(request.productNetwork);
            resolveEndpointRequest.setProductSuffix(request.productSuffix);
            String endpoint = endpointResolver.resolve(resolveEndpointRequest);
            domain = new ProductDomain(request.getSysProduct(), endpoint);

            if (endpoint.endsWith("endpoint-test.exception.com")) {
                // For endpoint testability, if the endpoint is xxxx.endpoint-test.special.com
                // throw a client exception with this endpoint
                throw new ClientException(ErrorCodeConstant.SDK_ENDPOINT_TESTABILITY, endpoint);
            }
        }

        return domain;
    }

    private <T extends AcsResponse> HttpResponse doAction(AcsRequest<T> request, boolean autoRetry, int maxRetryNumber,
                                                          String regionId, Credentials credentials, Signer signer, FormatType format)
            throws ClientException, ServerException {

        doActionWithProxy(request.getSysProtocol(), System.getenv("HTTPS_PROXY"), System.getenv("HTTP_PROXY"));
        doActionWithIgnoreSSL(request, X509TrustAll.ignoreSSLCerts);

        Logger logger = clientProfile.getLogger();
        String startTime = "";
        String timeCost = "";
        HttpResponse response = null;
        String errorMessage = "";
        try {
            FormatType requestFormatType = request.getSysAcceptFormat();
            if (null != requestFormatType) {
                format = requestFormatType;
            }

            ProductDomain domain = getDomain(request, regionId);

            if (request.getSysProtocol() == null) {
                request.setSysProtocol(this.clientProfile.getHttpClientConfig().getProtocolType());
            }
            request.putHeaderParameter("User-Agent",
                    UserAgentConfig.resolve(request.getSysUserAgentConfig(), this.userAgentConfig));
            try {
                HttpRequest httpRequest = request.signRequest(signer, credentials, format, domain);
                HttpUtil.debugHttpRequest(request);
                startTime = LogUtils.localeNow();
                long start = System.nanoTime();
                response = this.httpClient.syncInvoke(httpRequest);
                long end = System.nanoTime();
                timeCost = TimeUnit.NANOSECONDS.toMillis(end - start) + "ms";
                HttpUtil.debugHttpResponse(response);
                return response;
            } catch (SocketTimeoutException exp) {
                errorMessage = exp.getMessage();
                throw new ClientException("SDK.ReadTimeout",
                        "SocketTimeoutException has occurred on a socket read or accept.The url is " +
                                request.getSysUrl(), exp);
            } catch (IOException exp) {
                errorMessage = exp.getMessage();
                throw new ClientException("SDK.ServerUnreachable",
                        "Server unreachable: connection " + request.getSysUrl() + " failed", exp);
            }
        } catch (InvalidKeyException exp) {
            errorMessage = exp.getMessage();
            throw new ClientException("SDK.InvalidAccessSecret", "Specified access secret is not valid.", exp);
        } catch (NoSuchAlgorithmException exp) {
            errorMessage = exp.getMessage();
            throw new ClientException("SDK.InvalidMD5Algorithm", "MD5 hash is not supported by client side.", exp);
        } finally {
            if (null != logger) {
                try {
                    LogUtils.LogUnit logUnit = LogUtils.createLogUnit(request, response);
                    logUnit.setStartTime(startTime);
                    logUnit.setCost(timeCost);
                    logUnit.setError(errorMessage);
                    String logContent = LogUtils.fillContent(clientProfile.getLogFormat(), logUnit);
                    logger.info(logContent);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 2019-01-03 change access control from private to protected, then subClass can
     * override it and rewrite httpResponse processing
     */
    protected <T extends AcsResponse> T readResponse(Class<T> clasz, HttpResponse httpResponse, FormatType format)
            throws ClientException {
        // new version response contains "@XmlRootElement" annotation
        if (clasz.isAnnotationPresent(XmlRootElement.class)
                && !clientProfile.getHttpClientConfig().isCompatibleMode()) {
            Unmarshaller unmarshaller = UnmarshallerFactory.getUnmarshaller(format);
            return unmarshaller.unmarshal(clasz, httpResponse.getHttpContentString());
        } else {
            Reader reader = ReaderFactory.createInstance(format);
            UnmarshallerContext context = new UnmarshallerContext();
            T response = null;
            String stringContent = httpResponse.getHttpContentString();

            if (stringContent == null) {
                throw new ClientException(ErrorCodeConstant.SDK_INVALID_SERVER_RESPONSE,
                        ErrorMessageConstant.SERVER_RESPONSE_HTTP_BODY_EMPTY);
            }

            try {
                response = clasz.newInstance();
            } catch (Exception e) {
                throw new ClientException("SDK.InvalidResponseClass",
                        "Unable to allocate " + clasz.getName() + " class");
            }

            String responseEndpoint = clasz.getName().substring(clasz.getName().lastIndexOf(".") + 1);
            if (response.checkShowJsonItemName()) {
                context.setResponseMap(reader.read(stringContent, responseEndpoint));
            } else {
                context.setResponseMap(reader.readForHideArrayItem(stringContent, responseEndpoint));
            }

            context.setHttpResponse(httpResponse);
            response.getInstance(context);
            return response;
        }
    }

    private AcsError readError(HttpResponse httpResponse, FormatType format) throws ClientException {
        AcsError error = new AcsError();
        String responseEndpoint = "Error";
        Reader reader = ReaderFactory.createInstance(format);
        UnmarshallerContext context = new UnmarshallerContext();
        String stringContent = httpResponse.getHttpContentString();

        if (stringContent == null) {
            error.setErrorCode("(null)");
            error.setErrorMessage("(null)");
            error.setRequestId("(null)");
            error.setStatusCode(httpResponse.getStatus());
            return error;
        } else {
            context.setResponseMap(reader.read(stringContent, responseEndpoint));
            return error.getInstance(context);
        }
    }

    /**
     * Compatible with previous versions of proxy Settings
     */
    public void doActionWithProxy(ProtocolType protocolType, String httpsProxy, String httpProxy) {
        HttpClientConfig config = this.clientProfile.getHttpClientConfig();
        if (protocolType == ProtocolType.HTTP && httpProxy != null) {
            config.setHttpProxy(httpProxy);
            return;
        }
        if (protocolType == ProtocolType.HTTPS && httpsProxy != null) {
            config.setHttpsProxy(httpsProxy);
            return;
        }
    }

    /**
     * Compatible with previous versions of SSL Settings
     */
    public void doActionWithIgnoreSSL(AcsRequest request, boolean isIgnore) {
        if (isIgnore) {
            request.setIgnoreSSLCerts(true);
        }
    }







    @Override
    public void restoreSSLCertificate() {
        this.httpClient.restoreSSLCertificate();
    }

    @Override
    public void ignoreSSLCertificate() {
        this.httpClient.ignoreSSLCertificate();
    }

    public void setEndpointResolver(EndpointResolver resolver) {
        endpointResolver = resolver;
    }

    @Override
    public void shutdown() {
        if (!this.httpClient.isSingleton()) {
            IOUtils.closeQuietly(this.httpClient);
            this.httpClient = null;
        }

    }

    public DefaultProfile getProfile() {
        return (DefaultProfile) clientProfile;
    }

    public void appendUserAgent(String key, String value) {
        this.userAgentConfig.append(key, value);
    }

    public UserAgentConfig getUserAgentConfig() {
        return userAgentConfig;
    }

    public IHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
