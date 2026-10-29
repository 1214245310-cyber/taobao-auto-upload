package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.AddForbiddenWordRequest;
import com.loa.dto.UpdateForbiddenWordRequest;
import com.loa.service.ForbiddenWordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/forbidden-word")
public class ForbiddenWordController {

    private final ForbiddenWordService service;

    public ForbiddenWordController(ForbiddenWordService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(service.list(authorization));
    }

    @PostMapping("/add")
    public R<Map<String, Object>> add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AddForbiddenWordRequest req) {
        return R.ok(service.add(authorization, req));
    }

    /** 批量导入：body 支持 text（按行拆分）或 lines 数组 */
    @PostMapping("/batch-add")
    public R<Map<String, Object>> batchAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        List<String> lines = new java.util.ArrayList<>();
        Object text = body.get("text");
        if (text != null) {
            for (String line : String.valueOf(text).split("\\r?\\n")) lines.add(line);
        }
        Object arr = body.get("lines");
        if (arr instanceof List<?> list) {
            for (Object o : list) lines.add(String.valueOf(o));
        }
        Object category = body.get("category");
        Object matchType = body.get("matchType");
        Object remark = body.get("remark");
        return R.ok(service.batchAdd(authorization, lines,
                category == null ? null : String.valueOf(category),
                matchType == null ? null : String.valueOf(matchType),
                remark == null ? null : String.valueOf(remark)));
    }

    @PostMapping("/update")    public R<Map<String, Object>> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateForbiddenWordRequest req) {
        return R.ok(service.update(authorization, req));
    }

    @PostMapping("/toggle")
    public R<Void> toggle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        boolean enabled = Boolean.parseBoolean(String.valueOf(body.get("enabled")));
        service.setEnabled(authorization, id, enabled);
        return R.ok();
    }

    @PostMapping("/delete")
    public R<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        service.delete(authorization, toLong(body.get("id")));
        return R.ok();
    }

    private Long toLong(Object v) {
        if (v == null) throw new IllegalArgumentException("缺少 id");
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id 格式不正确");
        }
    }
}
