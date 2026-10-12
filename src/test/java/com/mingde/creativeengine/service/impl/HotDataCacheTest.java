package com.mingde.creativeengine.service.impl;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.OccupationMapper;
import com.mingde.creativeengine.mapper.PolicyDataMapper;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.OccupationService;
import com.mingde.creativeengine.service.PolicyDataService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 热点数据缓存测试：验证加缓存后重复读取只回源一次数据库。
 *
 * <p>用进程内 {@link ConcurrentMapCacheManager}（即 app.redis.enabled=false 时的默认实现）
 * 承载，开启 Redis 后语义一致，因此该测试对两种部署形态都成立。</p>
 */
class HotDataCacheTest {

    private AnnotationConfigApplicationContext context;
    private PolicyDataMapper policyDataMapper;
    private OccupationMapper occupationMapper;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();
        policyDataMapper = context.getBean(PolicyDataMapper.class);
        occupationMapper = context.getBean(OccupationMapper.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void policyListIsServedFromCacheOnSecondRead() {
        when(policyDataMapper.selectList(any())).thenReturn(List.of(policy(1L), policy(2L)));
        PolicyDataService service = context.getBean(PolicyDataService.class);

        assertEquals(2, service.getAllPolicies().size());
        assertEquals(2, service.getAllPolicies().size());

        verify(policyDataMapper, times(1)).selectList(any());
    }

    @Test
    void occupationTagsAreServedFromCacheOnSecondRead() {
        when(occupationMapper.getAllMajors()).thenReturn(List.of("计算机科学与技术"));
        OccupationService service = context.getBean(OccupationService.class);

        service.getAllMajors();
        service.getAllMajors();

        verify(occupationMapper, times(1)).getAllMajors();
    }

    private static PolicyData policy(long id) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        policy.setPolicyName("政策" + id);
        return policy;
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        PolicyDataService policyDataService() {
            return new PolicyDataServiceImpl();
        }

        @Bean
        OccupationService occupationService() {
            return new OccupationServiceImpl();
        }

        @Bean
        PolicyDataMapper policyDataMapper() {
            return mock(PolicyDataMapper.class);
        }

        @Bean
        OccupationMapper occupationMapper() {
            return mock(OccupationMapper.class);
        }

        @Bean
        DeepSeekService deepSeekService() {
            return mock(DeepSeekService.class);
        }
    }
}
