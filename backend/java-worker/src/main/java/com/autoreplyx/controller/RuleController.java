package com.autoreplyx.controller;

import com.autoreplyx.dto.RuleDto;
import com.autoreplyx.entity.AutoRule;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.AutoRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final AutoRuleRepository ruleRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<RuleDto>> list() {
        User user = getCurrentUser();
        List<AutoRule> rules = ruleRepository.findByUserIdOrderByPriorityAsc(user.getId());
        return ResponseEntity.ok(rules.stream().map(RuleDto::from).toList());
    }

    @PostMapping
    public ResponseEntity<RuleDto> create(@RequestBody RuleDto request) {
        User user = getCurrentUser();

        AutoRule rule = AutoRule.builder()
                .userId(user.getId())
                .name(request.getName())
                .matchType(request.getMatchType())
                .keywords(request.getKeywords())
                .responseTemplate(request.getResponseTemplate())
                .priority(request.getPriority())
                .channel(request.getChannel())
                .cooldownSeconds(request.getCooldownSeconds() != null ? request.getCooldownSeconds() : 60)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ruleRepository.save(rule);
        return ResponseEntity.ok(RuleDto.from(rule));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        User user = getCurrentUser();
        return ruleRepository.findById(id)
                .filter(rule -> rule.getUserId().equals(user.getId()))
                .map(rule -> ResponseEntity.ok(RuleDto.from(rule)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RuleDto request) {
        User user = getCurrentUser();

        return ruleRepository.findById(id)
                .filter(rule -> rule.getUserId().equals(user.getId()))
                .map(rule -> {
                    if (request.getName() != null) rule.setName(request.getName());
                    if (request.getMatchType() != null) rule.setMatchType(request.getMatchType());
                    if (request.getKeywords() != null) rule.setKeywords(request.getKeywords());
                    if (request.getResponseTemplate() != null) rule.setResponseTemplate(request.getResponseTemplate());
                    if (request.getPriority() != null) rule.setPriority(request.getPriority());
                    if (request.getChannel() != null) rule.setChannel(request.getChannel());
                    if (request.getCooldownSeconds() != null) rule.setCooldownSeconds(request.getCooldownSeconds());
                    if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());

                    ruleRepository.save(rule);
                    return ResponseEntity.ok(RuleDto.from(rule));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User user = getCurrentUser();

        return ruleRepository.findById(id)
                .filter(rule -> rule.getUserId().equals(user.getId()))
                .map(rule -> {
                    ruleRepository.delete(rule);
                    return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        User user = getCurrentUser();

        return ruleRepository.findById(id)
                .filter(rule -> rule.getUserId().equals(user.getId()))
                .map(rule -> {
                    rule.setIsActive(!rule.getIsActive());
                    ruleRepository.save(rule);
                    return ResponseEntity.ok(RuleDto.from(rule));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String message = request.get("message");
        String channel = request.get("channel");

        List<AutoRule> rules = ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(user.getId());

        for (AutoRule rule : rules) {
            if (rule.matches(message)) {
                return ResponseEntity.ok(Map.of(
                        "matched", true,
                        "rule", RuleDto.from(rule),
                        "response", rule.getResponseTemplate()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "matched", false,
                "message", "매칭되는 규칙이 없습니다. AI 응답이 생성됩니다."
        ));
    }

    @PostMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestBody List<Map<String, Object>> orders) {
        User user = getCurrentUser();

        for (Map<String, Object> order : orders) {
            Long ruleId = ((Number) order.get("id")).longValue();
            Integer priority = ((Number) order.get("priority")).intValue();

            ruleRepository.findById(ruleId)
                    .filter(rule -> rule.getUserId().equals(user.getId()))
                    .ifPresent(rule -> {
                        rule.setPriority(priority);
                        ruleRepository.save(rule);
                    });
        }

        List<AutoRule> rules = ruleRepository.findByUserIdOrderByPriorityAsc(user.getId());
        return ResponseEntity.ok(rules.stream().map(RuleDto::from).toList());
    }
}
