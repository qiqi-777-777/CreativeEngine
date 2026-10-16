package com.mingde.creativeengine.service;

import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import java.util.List;

/**
 * Policy data service.
 */
public interface PolicyDataService {

    List<PolicyData> getAllPolicies();

    PolicyData getPolicyById(Long id);

    PolicyData getPolicyByName(String policyName);

    List<PolicyData> searchPolicies(String keyword);

    /**
     * 分页查询政策（带可选关键词过滤，语义与 {@link #searchPolicies} 一致）。
     *
     * <p>{@code searchPolicies} 是全量返回：小程序依赖它的 {@code data} 是数组、
     * 并且在客户端自己过滤，所以不能改；但数据量上来后全量返回会顶不住。
     * 这里提供走数据库分页的替代接口，翻页在 SQL 层完成，不把整表读进内存。</p>
     */
    PageResult<PolicyData> pagePolicies(String keyword, int page, int size);
}
