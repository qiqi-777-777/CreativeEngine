package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.gson.Gson;
import com.mingde.creativeengine.entity.UserBpDraft;
import com.mingde.creativeengine.entity.UserCareerRecord;
import com.mingde.creativeengine.entity.UserNotification;
import com.mingde.creativeengine.entity.UserPolicyFavorite;
import com.mingde.creativeengine.lock.DistributedLock;
import com.mingde.creativeengine.mapper.UserBpDraftMapper;
import com.mingde.creativeengine.mapper.UserCareerRecordMapper;
import com.mingde.creativeengine.mapper.UserNotificationMapper;
import com.mingde.creativeengine.mapper.UserPolicyFavoriteMapper;
import com.mingde.creativeengine.service.UserDataService;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人数据服务实现。
 *
 * <p><b>为什么这里不加 {@code @Transactional}</b>：三处「先查后写」的互斥靠
 * {@link DistributedLock}，而事务必须开在锁里面——先开事务再等锁，会一直占着数据库连接
 * 等锁，并发一上来连接池就满了。声明式 {@code @Transactional} 只能加在方法最外层，
 * 顺序正好是反的；要放进锁内得用编程式事务（{@code TransactionTemplate}）。
 * 当前每次写入只碰一张表，单条 INSERT/UPDATE 自带原子性，所以先不加；
 * 以后出现「收藏政策 + 写通知」这类跨表写，再换 TransactionTemplate 包在锁内。</p>
 */
@Service
public class UserDataServiceImpl implements UserDataService {

    /** 抢锁最长等待时间：用户连点两次的间隔量级。 */
    private static final Duration LOCK_WAIT = Duration.ofSeconds(3);

    /** 持锁上限：即使业务线程异常退出也能自动释放，避免死锁。 */
    private static final Duration LOCK_LEASE = Duration.ofSeconds(10);

    private static final Gson GSON = new Gson();

    private final UserBpDraftMapper bpDraftMapper;
    private final UserCareerRecordMapper careerRecordMapper;
    private final UserPolicyFavoriteMapper policyFavoriteMapper;
    private final UserNotificationMapper notificationMapper;
    private final DistributedLock distributedLock;

    public UserDataServiceImpl(UserBpDraftMapper bpDraftMapper,
                               UserCareerRecordMapper careerRecordMapper,
                               UserPolicyFavoriteMapper policyFavoriteMapper,
                               UserNotificationMapper notificationMapper,
                               DistributedLock distributedLock) {
        this.bpDraftMapper = bpDraftMapper;
        this.careerRecordMapper = careerRecordMapper;
        this.policyFavoriteMapper = policyFavoriteMapper;
        this.notificationMapper = notificationMapper;
        this.distributedLock = distributedLock;
    }

    @Override
    public UserDataStats stats(Long userId) {
        return new UserDataStats(
                policyFavoriteMapper.selectCount(policyFavoriteQuery(userId)),
                careerRecordMapper.selectCount(careerRecordQuery(userId)),
                bpDraftMapper.selectCount(bpDraftQuery(userId)),
                notificationMapper.selectCount(notificationQuery(userId)),
                notificationMapper.selectCount(notificationQuery(userId).eq("is_read", 0)));
    }

    @Override
    public List<UserBpDraft> listBpDrafts(Long userId) {
        return bpDraftMapper.selectList(bpDraftQuery(userId).orderByDesc("update_time").orderByDesc("id"));
    }

    @Override
    public UserBpDraft saveBpDraft(Long userId, Map<String, Object> data) {
        String draftKey = text(data.get("id"), "bp-" + System.currentTimeMillis());
        // 「先查后写」是临界区：并发点两次会各查到空、各插一条，用分布式锁互斥
        return distributedLock.execute(
                "ce:lock:user-data:bp-draft:" + userId + ":" + draftKey,
                LOCK_WAIT, LOCK_LEASE,
                () -> doSaveBpDraft(userId, draftKey, data));
    }

    @Override
    public void deleteBpDraft(Long userId, String draftKey) {
        bpDraftMapper.delete(bpDraftQuery(userId).eq("draft_key", draftKey));
    }

    @Override
    public List<UserCareerRecord> listCareerRecords(Long userId) {
        return careerRecordMapper.selectList(careerRecordQuery(userId).orderByDesc("create_time").orderByDesc("id"));
    }

    @Override
    public UserCareerRecord saveCareerRecord(Long userId, Map<String, Object> data) {
        String recordKey = text(data.get("id"), "career-" + System.currentTimeMillis());
        // 同上：先查后写需要互斥，否则并发提交会写出多条记录
        return distributedLock.execute(
                "ce:lock:user-data:career-record:" + userId + ":" + recordKey,
                LOCK_WAIT, LOCK_LEASE,
                () -> doSaveCareerRecord(userId, recordKey, data));
    }

    @Override
    public void deleteCareerRecord(Long userId, String recordKey) {
        careerRecordMapper.delete(careerRecordQuery(userId).eq("record_key", recordKey));
    }

    @Override
    public List<UserPolicyFavorite> listPolicyFavorites(Long userId) {
        return policyFavoriteMapper.selectList(
                policyFavoriteQuery(userId).orderByDesc("create_time").orderByDesc("id"));
    }

    @Override
    public UserPolicyFavorite savePolicyFavorite(Long userId, Map<String, Object> data) {
        String policyName = text(data.get("policyName"), "政策详情");
        // 同一用户对同一政策的收藏也是「先查后写」，同样需要互斥
        return distributedLock.execute(
                "ce:lock:user-data:policy-favorite:" + userId + ":" + policyName,
                LOCK_WAIT, LOCK_LEASE,
                () -> doSavePolicyFavorite(userId, policyName, data));
    }

    @Override
    public void deletePolicyFavorite(Long userId, String key) {
        QueryWrapper<UserPolicyFavorite> wrapper = policyFavoriteQuery(userId);
        wrapper.and((query) -> {
            query.eq("policy_name", key);
            Long policyId = toLong(key);
            if (policyId != null) {
                query.or().eq("policy_id", policyId);
            }
        });
        policyFavoriteMapper.delete(wrapper);
    }

    @Override
    public List<UserNotification> listNotifications(Long userId) {
        return notificationMapper.selectList(
                notificationQuery(userId).orderByDesc("create_time").orderByDesc("id"));
    }

    @Override
    public UserNotification saveNotification(Long userId, Map<String, Object> data) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(text(data.get("title"), "系统通知"));
        notification.setType(text(data.get("type"), "通知"));
        notification.setContent(text(data.get("content"), ""));
        notification.setReadStatus(Boolean.TRUE.equals(data.get("read")) ? 1 : 0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }

    @Override
    public void markNotificationRead(Long userId, Long id) {
        notificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .eq("user_id", userId)
                .eq("id", id)
                .set("is_read", 1));
    }

    @Override
    public void markAllNotificationsRead(Long userId) {
        notificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .eq("user_id", userId)
                .set("is_read", 1));
    }

    private UserBpDraft doSaveBpDraft(Long userId, String draftKey, Map<String, Object> data) {
        UserBpDraft draft = bpDraftMapper.selectOne(
                bpDraftQuery(userId).eq("draft_key", draftKey).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        boolean isNew = draft == null;
        if (isNew) {
            draft = new UserBpDraft();
            draft.setUserId(userId);
            draft.setDraftKey(draftKey);
            draft.setCreateTime(now);
        }

        Object formData = data.get("formData");
        draft.setProjectName(text(data.get("projectName"), "未命名项目"));
        draft.setOneLiner(text(data.get("oneLiner"), ""));
        draft.setFormData(GSON.toJson(formData == null ? new HashMap<>() : formData));
        draft.setContent(text(data.get("content"), ""));
        draft.setUpdateTime(now);

        if (isNew) {
            bpDraftMapper.insert(draft);
        } else {
            bpDraftMapper.updateById(draft);
        }
        return draft;
    }

    private UserCareerRecord doSaveCareerRecord(Long userId, String recordKey, Map<String, Object> data) {
        UserCareerRecord record = careerRecordMapper.selectOne(
                careerRecordQuery(userId).eq("record_key", recordKey).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        boolean isNew = record == null;
        if (isNew) {
            record = new UserCareerRecord();
            record.setUserId(userId);
            record.setRecordKey(recordKey);
            record.setCreateTime(now);
        }

        Object results = data.get("results");
        record.setResultData(GSON.toJson(results == null ? new ArrayList<>() : results));
        record.setUpdateTime(now);

        if (isNew) {
            careerRecordMapper.insert(record);
        } else {
            careerRecordMapper.updateById(record);
        }
        return record;
    }

    private UserPolicyFavorite doSavePolicyFavorite(Long userId, String policyName, Map<String, Object> data) {
        UserPolicyFavorite favorite = policyFavoriteMapper.selectOne(
                policyFavoriteQuery(userId).eq("policy_name", policyName).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        boolean isNew = favorite == null;
        if (isNew) {
            favorite = new UserPolicyFavorite();
            favorite.setUserId(userId);
            favorite.setCreateTime(now);
        }

        favorite.setPolicyId(toLong(data.get("id")));
        favorite.setPolicyName(policyName);
        favorite.setPolicySnapshot(GSON.toJson(data));

        if (isNew) {
            policyFavoriteMapper.insert(favorite);
        } else {
            policyFavoriteMapper.updateById(favorite);
        }
        return favorite;
    }

    private QueryWrapper<UserBpDraft> bpDraftQuery(Long userId) {
        return new QueryWrapper<UserBpDraft>().eq("user_id", userId);
    }

    private QueryWrapper<UserCareerRecord> careerRecordQuery(Long userId) {
        return new QueryWrapper<UserCareerRecord>().eq("user_id", userId);
    }

    private QueryWrapper<UserPolicyFavorite> policyFavoriteQuery(Long userId) {
        return new QueryWrapper<UserPolicyFavorite>().eq("user_id", userId);
    }

    private QueryWrapper<UserNotification> notificationQuery(Long userId) {
        return new QueryWrapper<UserNotification>().eq("user_id", userId);
    }

    /** 取字符串，空值与空白都退回默认值。 */
    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
