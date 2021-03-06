package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.http.CsrfHttpClient;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.ShutdownClientConfiguration;

public class ShutdownClientFactory {

    private static final String CSRF_TOKEN_ENDPOINT = "/api/v1/csrf-token";
    private static final int RETRY_COUNT = 5;
    private static final int RETRY_INTERVAL_IN_MILLIS = 5000;

    public ShutdownClient createShutdownClient(ShutdownClientConfiguration configuration) {
        return new ShutdownClientImpl(configuration.getApplicationUrl(),
                                      defaultHttpHeaders -> createCsrfHttpClient(configuration, defaultHttpHeaders));

    }

    private CsrfHttpClient createCsrfHttpClient(ShutdownClientConfiguration configuration, Map<String, String> defaultHttpHeaders) {
        CloseableHttpClient httpClient = createHttpClient();
        String csrfTokenUrl = computeCsrfTokenUrl(configuration);
        Map<String, String> enrichedDefaultHttpHeaders = MapUtil.merge(computeHeaders(configuration), defaultHttpHeaders);
        return new CsrfHttpClient(httpClient, csrfTokenUrl, enrichedDefaultHttpHeaders);
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                                .setServiceUnavailableRetryStrategy(createServiceUnavailableRetryStrategy())
                                .build();
    }

    private ServiceUnavailableRetryStrategy createServiceUnavailableRetryStrategy() {
        return new DefaultServiceUnavailableRetryStrategy(RETRY_COUNT, RETRY_INTERVAL_IN_MILLIS);
    }

    private String computeCsrfTokenUrl(ShutdownClientConfiguration configuration) {
        return configuration.getApplicationUrl() + CSRF_TOKEN_ENDPOINT;
    }

    private Map<String, String> computeHeaders(ShutdownClientConfiguration configuration) {
        String credentials = computeBasicAuthorizationCredentials(configuration);
        return Map.of(HttpHeaders.AUTHORIZATION, String.format("Basic %s", encode(credentials)));
    }

    private String encode(String string) {
        return Base64.getEncoder()
                     .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private String computeBasicAuthorizationCredentials(ShutdownClientConfiguration configuration) {
        return String.format("%s:%s", configuration.getUsername(), configuration.getPassword());
    }

}
