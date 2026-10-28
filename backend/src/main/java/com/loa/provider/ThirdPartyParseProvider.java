package com.loa.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

@Component
public class ThirdPartyParseProvider implements ParseProvider {

    private final ObjectMapper objectMapper;

    @Value("${loa.parse.third-party.base-url:}")
    private String baseUrl;

    @Value("${loa.parse.third-party.access-token:}")
    private String accessToken;

    public ThirdPartyParseProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() { return "third-party"; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String url, String platform) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("第三方解析服务未配置：请在 application.yml 填写 loa.parse.third-party.base-url 与 access-token");
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "url", url == null ? "" : url,
                    "platform", platform == null ? "TAOBAO" : platform,
                    "token", accessToken == null ? "" : accessToken));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/parse"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = objectMapper.readValue(resp.body(), Map.class);
            Object data = body.get("data");
            if (data instanceof Map) return (Map<String, Object>) data;
            return body;
        } catch (Exception e) {
            throw new IllegalStateException("第三方解析调用失败: " + e.getMessage(), e);
        }
    }
}
