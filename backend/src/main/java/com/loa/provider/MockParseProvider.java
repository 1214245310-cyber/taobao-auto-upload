package com.loa.provider;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

@Component
public class MockParseProvider implements ParseProvider {

    private static final Pattern ID_PATTERN = Pattern.compile("id=([0-9]+)");

    @Override
    public String name() { return "mock"; }

    @Override
    public Map<String, Object> parse(String url, String platform) {
        String itemId = extractItemId(url);
        long seed = Math.abs((url == null ? "" : url).hashCode());
        double price = (1990 + (int) (seed % 48000)) / 100.0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sourceUrl", url);
        m.put("platform", platform);
        m.put("itemId", itemId);
        m.put("title", "【演示】" + platformName(platform) + "商品 " + itemId + " 自动解析样例");
        m.put("price", price);
        m.put("shopName", "LOA 演示店铺");
        m.put("images", List.of("https://placehold.co/800x800/png?text=LOA"));
        Map<String, Object> sku = new LinkedHashMap<>();
        sku.put("spec", "默认规格");
        sku.put("price", price);
        sku.put("stock", 100);
        m.put("sku", List.of(sku));
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("brand", "LOA");
        attrs.put("origin", "CN");
        m.put("attributes", attrs);
        m.put("parseTime", LocalDateTime.now().toString());
        m.put("provider", name());
        return m;
    }

    private String extractItemId(String url) {
        if (url == null) return "0";
        Matcher m = ID_PATTERN.matcher(url);
        if (m.find()) return m.group(1);
        return String.valueOf(Math.abs(url.hashCode()) % 1000000);
    }

    private String platformName(String p) {
        if (p == null) return "淘宝";
        switch (p) {
            case "ALI1688": return "1688";
            case "PDD": return "拼多多";
            case "JD": return "京东";
            default: return "淘宝";
        }
    }
}
