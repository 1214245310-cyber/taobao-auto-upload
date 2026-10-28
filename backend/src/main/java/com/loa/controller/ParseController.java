package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.ParseRequest;
import com.loa.service.ParseService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/goods")
public class ParseController {

    private final ParseService parseService;

    public ParseController(ParseService parseService) {
        this.parseService = parseService;
    }

    @PostMapping("/parse-item")
    public R<Map<String, Object>> parseItem(@RequestBody ParseRequest req) {
        return R.ok(parseService.parse(req.getUrl(), req.getPlatform()));
    }

    @GetMapping("/parse-channel/list")
    public R<List<Map<String, Object>>> channels() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(channel("TAOBAO", "淘宝"));
        list.add(channel("ALI1688", "1688"));
        list.add(channel("PDD", "拼多多"));
        list.add(channel("JD", "京东"));
        return R.ok(list);
    }

    @GetMapping("/parse-task/submit-context")
    public R<Map<String, Object>> submitContext() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("providers", List.of("mock", "third-party"));
        m.put("maxBatch", 50);
        return R.ok(m);
    }

    @PostMapping("/parse-task/upload")
    public R<Map<String, Object>> upload() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accepted", 0);
        m.put("message", "批量上传接口已预留");
        return R.ok(m);
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(parseService.list(page, size));
    }

    private Map<String, Object> channel(String value, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
    }
}
