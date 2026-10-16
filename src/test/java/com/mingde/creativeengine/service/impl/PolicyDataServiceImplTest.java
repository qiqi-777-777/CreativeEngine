package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 政策分页查询测试。
 *
 * <p>关键词切分与 like 条件沿用 {@code searchPolicies} 的规则，这里主要盯住
 * 「翻页交给数据库」和 {@code hasMore} 的边界。</p>
 */
class PolicyDataServiceImplTest {

    private final PolicyDataMapper policyDataMapper = mock(PolicyDataMapper.class);
    private final PolicyDataServiceImpl service = new PolicyDataServiceImpl();

    PolicyDataServiceImplTest() {
        ReflectionTestUtils.setField(service, "policyDataMapper", policyDataMapper);
    }

    @Test
    void returnsRecordsWithTotalAndHasMore() {
        stubPage(37, policy(1L), policy(2L));

        PageResult<PolicyData> result = service.pagePolicies(null, 1, 20);

        assertEquals(2, result.list().size());
        assertEquals(37L, result.total());
        assertEquals(1L, result.page());
        assertEquals(20L, result.size());
        assertTrue(result.hasMore(), "37 条按每页 20 分，第 1 页之后还有 17 条");
    }

    @Test
    void lastPageHasNoMore() {
        stubPage(30, policy(1L));

        PageResult<PolicyData> result = service.pagePolicies(null, 2, 20);

        assertFalse(result.hasMore(), "30 条按每页 20 分，第 2 页就是最后一页");
    }

    @Test
    void paginationIsDelegatedToDatabase() {
        stubPage(5, policy(1L));

        service.pagePolicies(null, 3, 10);

        ArgumentCaptor<Page<PolicyData>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(policyDataMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(3, pageCaptor.getValue().getCurrent(), "页码要透传给数据库，不能取全量再在内存里切");
        assertEquals(10, pageCaptor.getValue().getSize());
    }

    @Test
    @SuppressWarnings("unchecked")
    void keywordIsSplitIntoSeveralGroupsOfLikeConditions() {
        stubPage(1, policy(1L));

        // 「毕业生创业」按既有规则会拆成「毕业生」和「创业」两个词
        service.pagePolicies("毕业生创业", 1, 20);

        ArgumentCaptor<Wrapper<PolicyData>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(policyDataMapper).selectPage(any(), wrapperCaptor.capture());
        String segment = wrapperCaptor.getValue().getSqlSegment();

        assertEquals(2, countOccurrences(segment, "policy_name"),
                "两个关键词应生成两组条件：" + segment);
        assertTrue(segment.contains("keywords"), "关键词字段也要参与匹配：" + segment);
        assertTrue(segment.contains("content"), "正文也要参与匹配：" + segment);
    }

    private void stubPage(long total, PolicyData... records) {
        Page<PolicyData> returned = new Page<>(1, 20, total);
        returned.setRecords(List.of(records));
        when(policyDataMapper.selectPage(any(), any())).thenReturn(returned);
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int index = text.indexOf(target);
        while (index >= 0) {
            count++;
            index = text.indexOf(target, index + target.length());
        }
        return count;
    }

    private static PolicyData policy(Long id) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        policy.setPolicyName("政策-" + id);
        return policy;
    }
}
