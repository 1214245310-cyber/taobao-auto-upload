package com.loa.service;

import com.loa.dto.AddForbiddenWordRequest;
import com.loa.dto.UpdateForbiddenWordRequest;
import com.loa.entity.ForbiddenWord;
import com.loa.entity.User;
import com.loa.repository.ForbiddenWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 违禁词管理。所有登录用户均可查看与维护（新增/编辑/启停/删除）。 */
@Service
public class ForbiddenWordService {

    private final ForbiddenWordRepository repository;
    private final AuthService authService;

    public ForbiddenWordService(ForbiddenWordRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    /** 列表，按创建时间倒序 */
    public List<Map<String, Object>> list(String authorization) {
        authService.currentUser(authorization);
        List<ForbiddenWord> all = repository.findAll();
        all.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        List<Map<String, Object>> out = new ArrayList<>();
        for (ForbiddenWord w : all) out.add(toMap(w));
        return out;
    }

    @Transactional
    public Map<String, Object> add(String authorization, AddForbiddenWordRequest req) {
        User operator = authService.currentUser(authorization);
        String word = req.getWord().trim();
        if (repository.findByWord(word).isPresent()) {
            throw new IllegalArgumentException("违禁词「" + word + "」已存在");
        }
        ForbiddenWord entity = new ForbiddenWord(word, req.getCategory(), req.getLevel());
        entity.setRemark(req.getRemark());
        if (req.getEnabled() != null && !req.getEnabled().isBlank()) {
            entity.setEnabled("0".equals(req.getEnabled()) ? "0" : "1");
        }
        entity.setCreatedBy(operator.getUsername());
        return toMap(repository.save(entity));
    }

    /** 部分更新：未传的字段保持不变，便于表格内下拉直接改单个字段 */
    @Transactional
    public Map<String, Object> update(String authorization, UpdateForbiddenWordRequest req) {
        authService.currentUser(authorization);
        ForbiddenWord w = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("违禁词不存在"));
        if (req.getWord() != null && !req.getWord().isBlank()) {
            String word = req.getWord().trim();
            repository.findByWord(word).ifPresent(other -> {
                if (!other.getId().equals(w.getId())) {
                    throw new IllegalArgumentException("违禁词「" + word + "」已存在");
                }
            });
            w.setWord(word);
        }
        if (req.getCategory() != null) w.setCategory(req.getCategory());
        if (req.getLevel() != null) w.setLevel(req.getLevel());
        if (req.getRemark() != null) w.setRemark(req.getRemark());
        if (req.getEnabled() != null && !req.getEnabled().isBlank()) {
            w.setEnabled("0".equals(req.getEnabled()) ? "0" : "1");
        }
        return toMap(repository.save(w));
    }

    @Transactional
    public void setEnabled(String authorization, Long id, boolean enabled) {
        authService.currentUser(authorization);
        ForbiddenWord w = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("违禁词不存在"));
        w.setEnabled(enabled ? "1" : "0");
        repository.save(w);
    }

    @Transactional
    public void delete(String authorization, Long id) {
        authService.currentUser(authorization);
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("违禁词不存在");
        }
        repository.deleteById(id);
    }

    private Map<String, Object> toMap(ForbiddenWord w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("word", w.getWord());
        m.put("category", w.getCategory() == null ? "" : w.getCategory());
        m.put("level", w.getLevel() == null ? 2 : w.getLevel());
        m.put("enabled", w.getEnabled() == null ? "1" : w.getEnabled());
        m.put("remark", w.getRemark() == null ? "" : w.getRemark());
        m.put("createdBy", w.getCreatedBy() == null ? "system" : w.getCreatedBy());
        m.put("createdAt", w.getCreatedAt() == null ? "" : w.getCreatedAt().toString());
        return m;
    }
}
