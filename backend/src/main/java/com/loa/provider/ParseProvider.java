package com.loa.provider;

import java.util.Map;

/** 解析器抽象：Mock 与三方聚合适配器都实现它 */
public interface ParseProvider {
    String name();
    Map<String, Object> parse(String url, String platform);
}
