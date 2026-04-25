package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;
import com.mingde.creativeengine.service.PolicyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Policy data service implementation.
 */
@Service
public class PolicyDataServiceImpl implements PolicyDataService {

    private static final List<String> KNOWN_SEARCH_TERMS = Arrays.asList(
        "高校毕业生",
        "毕业生",
        "大学生",
        "创业"
    );

    @Autowired
    private PolicyDataMapper policyDataMapper;

    @Override
    public List<PolicyData> getAllPolicies() {
        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PolicyData::getCreateTime);
        return policyDataMapper.selectList(wrapper);
    }

    @Override
    public PolicyData getPolicyById(Long id) {
        return policyDataMapper.selectById(id);
    }

    @Override
    public PolicyData getPolicyByName(String policyName) {
        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyData::getPolicyName, policyName);
        return policyDataMapper.selectOne(wrapper);
    }

    @Override
    public List<PolicyData> searchPolicies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPolicies();
        }

        List<String> terms = parseSearchTerms(keyword);
        if (terms.isEmpty()) {
            return getAllPolicies();
        }

        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        for (String term : terms) {
            wrapper.and(w -> w
                .like(PolicyData::getPolicyName, term)
                .or()
                .like(PolicyData::getKeywords, term)
                .or()
                .like(PolicyData::getContent, term)
            );
        }
        wrapper.orderByDesc(PolicyData::getCreateTime);
        return policyDataMapper.selectList(wrapper);
    }

    private List<String> parseSearchTerms(String keyword) {
        String normalized = keyword.trim().replaceAll("[,，、+\\s;/；|]+", " ");
        Set<String> terms = new LinkedHashSet<>();

        for (String item : normalized.split(" ")) {
            addSearchTerm(item, terms);
        }

        return new ArrayList<>(terms);
    }

    private void addSearchTerm(String rawValue, Set<String> terms) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return;
        }

        boolean matchedKnownTerm = false;
        String remaining = value;
        for (String knownTerm : KNOWN_SEARCH_TERMS) {
            if (remaining.contains(knownTerm)) {
                terms.add(knownTerm);
                remaining = remaining.replace(knownTerm, " ");
                matchedKnownTerm = true;
            }
        }

        if (!matchedKnownTerm) {
            terms.add(value);
            return;
        }

        for (String item : remaining.trim().split("\\s+")) {
            if (item.trim().length() > 1) {
                terms.add(item.trim());
            }
        }
    }
}
