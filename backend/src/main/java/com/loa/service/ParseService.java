package com.loa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loa.entity.ForbiddenWord;
import com.loa.entity.ParsedItem;
import com.loa.entity.SourceLink;
import com.loa.provider.MockParseProvider;
import com.loa.provider.ParseProvider;
import com.loa.provider.ThirdPartyParseProvider;
import com.loa.repository.ForbiddenWordRepository;
import com.loa.repository.ParsedItemRepository;
import com.loa.repository.SourceLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ParseService {

    private final ParsedItemRepository parsedItemRepository;
    private final SourceLinkRepository sourceLinkRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;
    private final MockParseProvider mockProvider;
    private final ThirdPartyParseProvider thirdPartyProvider;
    private final ObjectMapper objectMapper;
    private final String providerName;

    public ParseService(ParsedItemRepository parsedItemRepository,
                        SourceLinkRepository sourceLinkRepository,
                        ForbiddenWordRepository forbiddenWordRepository,
                        MockParseProvider mockProvider,
                        ThirdPartyParseProvider thirdPartyProvider,
                        ObjectMapper objectMapper,
                        @Value("${loa.parse.provider:mock}") String providerName) {
        this.parsedItemRepository = parsedItemRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.forbiddenWordRepository = forbiddenWordRepository;
        this.mockProvider = mockProvider;
        this.thirdPartyProvider = thirdPartyProvider;
        this.objectMapper = objectMapper;
        this.providerName = providerName;
    }

    public Map<String, Object> parse(String url, String platform) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("商品链接不能为空");
        }
        String p = (platform == null || platform.isBlank()) ? "TAOBAO" : platform;
        ParseProvider provider = "third-party".equalsIgnoreCase(providerName) ? thirdPartyProvider : mockProvider;
        Map<String, Object> item = provider.parse(url, p);

        String title = String.valueOf(item.getOrDefault("title", ""));
        List<String> hits = checkForbidden(title);

        SourceLink link = new SourceLink();
        link.setUrl(url);
        link.setPlatform(p);
        link.setStatus("PARSED");
        link = sourceLinkRepository.save(link);

        ParsedItem record = new ParsedItem();
        record.setSourceLinkId(link.getId());
        record.setPlatform(p);
        record.setSourceUrl(url);
        record.setTitle(title);
        record.setPrice(String.valueOf(item.getOrDefault("price", "")));
        record.setShopName(String.valueOf(item.getOrDefault("shopName", "")));
        record.setStatus(hits.isEmpty() ? "PARSED" : "FORBIDDEN");
        record.setRawJson(toJson(item));
        record = parsedItemRepository.save(record);

        item.put("forbiddenHits", hits);
        item.put("recordId", record.getId());
        return item;
    }

    public Map<String, Object> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Page<ParsedItem> result = parsedItemRepository.findAllByOrderByIdDesc(pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ParsedItem item : result.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("platform", item.getPlatform());
            row.put("sourceUrl", item.getSourceUrl());
            row.put("title", item.getTitle());
            row.put("price", item.getPrice());
            row.put("shopName", item.getShopName());
            row.put("status", item.getStatus());
            row.put("createdAt", item.getCreatedAt() == null ? null : item.getCreatedAt().toString());
            rows.add(row);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", rows);
        m.put("total", result.getTotalElements());
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    private List<String> checkForbidden(String title) {
        List<String> hits = new ArrayList<>();
        if (title == null || title.isBlank()) return hits;
        for (ForbiddenWord word : forbiddenWordRepository.findAll()) {
            if (word.getWord() != null && !word.getWord().isBlank() && title.contains(word.getWord())) {
                hits.add(word.getWord());
            }
        }
        return hits;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
