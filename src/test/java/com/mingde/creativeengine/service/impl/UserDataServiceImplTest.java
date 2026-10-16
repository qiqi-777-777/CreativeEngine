package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mingde.creativeengine.entity.UserBpDraft;
import com.mingde.creativeengine.entity.UserCareerRecord;
import com.mingde.creativeengine.entity.UserNotification;
import com.mingde.creativeengine.entity.UserPolicyFavorite;
import com.mingde.creativeengine.lock.DistributedLock;
import com.mingde.creativeengine.mapper.UserBpDraftMapper;
import com.mingde.creativeengine.mapper.UserCareerRecordMapper;
import com.mingde.creativeengine.mapper.UserNotificationMapper;
import com.mingde.creativeengine.mapper.UserPolicyFavoriteMapper;
import com.mingde.creativeengine.service.UserDataService.UserDataStats;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人数据服务测试。
 *
 * <p>拆出服务层最直接的收益就在这类测试上：以前这些逻辑长在 Controller 里，
 * 想验证「有则更新、无则新增」得起一套 MVC 环境；现在注入几个 mock 就能测。</p>
 *
 * <p>锁被替换成直接执行临界区，锁本身的行为由 LocalDistributedLockTest / 集成测试覆盖。</p>
 */
class UserDataServiceImplTest {

    private final UserBpDraftMapper bpDraftMapper = mock(UserBpDraftMapper.class);
    private final UserCareerRecordMapper careerRecordMapper = mock(UserCareerRecordMapper.class);
    private final UserPolicyFavoriteMapper policyFavoriteMapper = mock(UserPolicyFavoriteMapper.class);
    private final UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
    private final DistributedLock distributedLock = mock(DistributedLock.class);

    private final UserDataServiceImpl service = new UserDataServiceImpl(
            bpDraftMapper, careerRecordMapper, policyFavoriteMapper, notificationMapper, distributedLock);

    UserDataServiceImplTest() {
        when(distributedLock.execute(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
    }

    @Test
    void saveBpDraftInsertsWhenAbsent() {
        when(bpDraftMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> request = new HashMap<>();
        request.put("id", "bp-1");
        request.put("projectName", "校园二手书平台");
        request.put("formData", Map.of("stage", "MVP"));

        UserBpDraft saved = service.saveBpDraft(7L, request);

        ArgumentCaptor<UserBpDraft> captor = ArgumentCaptor.forClass(UserBpDraft.class);
        verify(bpDraftMapper).insert(captor.capture());
        verify(bpDraftMapper, never()).updateById(any(UserBpDraft.class));

        UserBpDraft inserted = captor.getValue();
        assertEquals(7L, inserted.getUserId());
        assertEquals("bp-1", inserted.getDraftKey());
        assertEquals("校园二手书平台", inserted.getProjectName());
        assertTrue(inserted.getFormData().contains("MVP"), "formData 应序列化成 JSON：" + inserted.getFormData());
        assertSame(inserted, saved, "应把落库后的同一个对象返回给上层");
    }

    @Test
    void saveBpDraftUpdatesWhenPresent() {
        UserBpDraft existing = new UserBpDraft();
        existing.setId(99L);
        existing.setDraftKey("bp-1");
        existing.setUserId(7L);
        existing.setCreateTime(LocalDateTime.now().minusDays(1));
        when(bpDraftMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> request = new HashMap<>();
        request.put("id", "bp-1");
        request.put("projectName", "改了名字");

        UserBpDraft saved = service.saveBpDraft(7L, request);

        verify(bpDraftMapper).updateById(existing);
        // MyBatis-Plus 的 insert 同时有 insert(T) 与 insert(Collection<T>)，any() 会歧义，要指明类型
        verify(bpDraftMapper, never()).insert(any(UserBpDraft.class));
        assertEquals(99L, saved.getId(), "更新时应保留原主键，而不是当成新记录");
        assertEquals("改了名字", saved.getProjectName());
    }

    @Test
    void saveBpDraftTakesLockScopedByUserAndDraftKey() {
        when(bpDraftMapper.selectOne(any())).thenReturn(null);

        service.saveBpDraft(7L, Map.of("id", "bp-1"));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(distributedLock).execute(key.capture(), any(), any(), any());
        assertTrue(key.getValue().endsWith(":7:bp-1"),
                "锁键要精确到「某个用户的某份草稿」，否则会互相挡：" + key.getValue());
    }

    @Test
    void saveCareerRecordSerializesResults() {
        when(careerRecordMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> request = new HashMap<>();
        request.put("id", "career-1");
        request.put("results", List.of(Map.of("title", "后端开发")));

        service.saveCareerRecord(7L, request);

        ArgumentCaptor<UserCareerRecord> captor = ArgumentCaptor.forClass(UserCareerRecord.class);
        verify(careerRecordMapper).insert(captor.capture());
        assertTrue(captor.getValue().getResultData().contains("后端开发"));
    }

    @Test
    void savePolicyFavoriteKeepsSnapshotAndPolicyId() {
        when(policyFavoriteMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> request = new HashMap<>();
        request.put("id", 12);
        request.put("policyName", "高校毕业生一次性创业补贴");
        request.put("amount", "10000 元");

        service.savePolicyFavorite(7L, request);

        ArgumentCaptor<UserPolicyFavorite> captor = ArgumentCaptor.forClass(UserPolicyFavorite.class);
        verify(policyFavoriteMapper).insert(captor.capture());
        UserPolicyFavorite saved = captor.getValue();
        assertEquals(12L, saved.getPolicyId());
        assertTrue(saved.getPolicySnapshot().contains("10000 元"), "整份快照要存下来，列表页才能原样回显");
    }

    @Test
    void saveNotificationDefaultsTitleAndType() {
        Map<String, Object> request = new HashMap<>();
        request.put("content", "你的简历已生成");
        request.put("read", true);

        service.saveNotification(7L, request);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).insert(captor.capture());
        UserNotification saved = captor.getValue();
        assertEquals("系统通知", saved.getTitle(), "标题缺省时应有默认值");
        assertEquals("通知", saved.getType());
        assertEquals(1, saved.getReadStatus());
    }

    @Test
    void statsCountsEachTypeAndUnread() {
        when(policyFavoriteMapper.selectCount(any())).thenReturn(3L);
        when(careerRecordMapper.selectCount(any())).thenReturn(2L);
        when(bpDraftMapper.selectCount(any())).thenReturn(1L);
        // 通知查两次：总数、未读数
        when(notificationMapper.selectCount(any())).thenReturn(5L, 2L);

        UserDataStats stats = service.stats(7L);

        assertEquals(3L, stats.policyFavorites());
        assertEquals(2L, stats.careerRecords());
        assertEquals(1L, stats.bpDrafts());
        assertEquals(5L, stats.notifications());
        assertEquals(2L, stats.unreadNotifications(), "未读数取第二次（带 is_read=0 条件）的结果");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletePolicyFavoriteMatchesByNameOrId() {
        service.deletePolicyFavorite(7L, "12");

        ArgumentCaptor<Wrapper<UserPolicyFavorite>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(policyFavoriteMapper).delete(captor.capture());

        String segment = captor.getValue().getSqlSegment();
        assertTrue(segment.contains("policy_name"), "应支持按政策名删除：" + segment);
        assertTrue(segment.contains("policy_id"), "key 是数字时也应能按 ID 删除：" + segment);
    }
}
