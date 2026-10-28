package com.loa.controller;

import com.loa.common.R;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/tb/publish")
public class PublishController {

    @PostMapping("/list")
    public R<Map<String, Object>> list(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", Collections.emptyList());
        m.put("total", 0);
        return R.ok(m);
    }

    @PostMapping("/stat")
    public R<Map<String, Object>> stat(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pending", 0);
        m.put("running", 0);
        m.put("success", 0);
        m.put("failed", 0);
        return R.ok(m);
    }

    @GetMapping("/limits")
    public R<Map<String, Object>> limits() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dailyLimit", 100);
        m.put("used", 0);
        return R.ok(m);
    }

    @GetMapping("/operators")
    public R<List<String>> operators() {
        return R.ok(List.of("admin"));
    }

    @PutMapping("/{id}/restart")
    public R<Void> restart(@PathVariable Long id) { return R.ok(); }

    @PutMapping("/{id}/pause")
    public R<Void> pause(@PathVariable Long id) { return R.ok(); }

    @PutMapping("/{id}/resume")
    public R<Void> resume(@PathVariable Long id) { return R.ok(); }

    @PutMapping("/{id}/terminate")
    public R<Void> terminate(@PathVariable Long id) { return R.ok(); }
}
