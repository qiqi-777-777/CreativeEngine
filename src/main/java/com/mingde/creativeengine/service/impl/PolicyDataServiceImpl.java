package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;
import com.mingde.creativeengine.service.PolicyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Policy data service implementation.
 *
 * <p>政策数据读多写少且由外部脚本批量导入，最新政策列表与单条详情是最常被
 * 首页、政策页与 Agent 工具读取的数据，这里加上缓存：默认走进程内实现，
 * 开启 Redis 后由 {@code RedisCacheManager} 承载，多实例共享同一份缓存。</p>
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

    /**
     * 自我引用（延迟注入，避免循环依赖）。
     *
     * <p>{@code @Cacheable} 依赖 Spring 代理生效，类内部直接 {@code this.getAllPolicies()}
     * 会绕过代理，导致 {@link #searchPolicies} 在无关键词时走的是没缓存的版本。
     * 通过代理引用调用，缓存才会命中。</p>
     */
    @Lazy
    @Autowired
    private PolicyDataService self;

    @Override
    @Cacheable(cacheNames = "policyList")
    public List<PolicyData> getAllPolicies() {
        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PolicyData::getCreateTime);
        return policyDataMapper.selectList(wrapper);
    }

    /**
     * 按 id 查政策，结果进 L1/L2 缓存。
     *
     * <p>{@code unless = "#result == null"} 不能省：Redis 的缓存实现不允许存 null，
     * 查一个不存在的 id 会直接抛 {@code IllegalArgumentException} 把请求打成 500，
     * 「政策不存在」这个正常分支就走不到了。顺带也避免了把「不存在」这个瞬时状态缓存成 10 分钟。</p>
     */
    @Override
    @Cacheable(cacheNames = "policyDetail", key = "#id", unless = "#result == null")
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
            return self.getAllPolicies();
        }

        List<String> terms = parseSearchTerms(keyword);
        if (terms.isEmpty()) {
            return self.getAllPolicies();
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

    @Override
    public PageResult<PolicyData> pagePolicies(String keyword, int page, int size) {
        // 刻意不走 getAllPolicies 的缓存：单页查询复用全量缓存就又变成全量加载了，
        // 那就等于没分页。翻页交给数据库，代价是每次回源，换来的是内存与响应体都只跟页大小有关
        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        for (String term : parseSearchTerms(keyword == null ? "" : keyword)) {
            wrapper.and(w -> w
                .like(PolicyData::getPolicyName, term)
                .or()
                .like(PolicyData::getKeywords, term)
                .or()
                .like(PolicyData::getContent, term)
            );
        }
        wrapper.orderByDesc(PolicyData::getCreateTime);

        Page<PolicyData> result = policyDataMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
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
