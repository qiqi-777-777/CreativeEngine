package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.UserBpDraft;
import com.mingde.creativeengine.entity.UserCareerRecord;
import com.mingde.creativeengine.entity.UserNotification;
import com.mingde.creativeengine.entity.UserPolicyFavorite;

import java.util.List;
import java.util.Map;

/**
 * 用户个人数据服务：BP 草稿、职业匹配记录、政策收藏、通知。
 *
 * <p>改造前这些逻辑全写在 {@code UserDataController} 里——控制器直接注入四个 Mapper、
 * 自己拼 QueryWrapper、自己把实体转成响应 Map，业务和视图混在一起，
 * 想给「先查后写」写个单测都得起 MVC。现在按「控制器只管参数与响应」拆开。</p>
 *
 * <p>注意校验、鉴权都不在这一层：登录态由 {@code AuthInterceptor} 统一处理，
 * 参数在控制器里转成实体字段。这里只做业务与持久化。</p>
 */
public interface UserDataService {

    /** 各项个人数据的数量统计（含未读通知数）。 */
    UserDataStats stats(Long userId);

    List<UserBpDraft> listBpDrafts(Long userId);

    /** 按 draftKey 有则更新、无则新建，返回落库后的实体。 */
    UserBpDraft saveBpDraft(Long userId, Map<String, Object> data);

    void deleteBpDraft(Long userId, String draftKey);

    List<UserCareerRecord> listCareerRecords(Long userId);

    UserCareerRecord saveCareerRecord(Long userId, Map<String, Object> data);

    void deleteCareerRecord(Long userId, String recordKey);

    List<UserPolicyFavorite> listPolicyFavorites(Long userId);

    UserPolicyFavorite savePolicyFavorite(Long userId, Map<String, Object> data);

    /** 按政策名或政策 ID 取消收藏。 */
    void deletePolicyFavorite(Long userId, String key);

    List<UserNotification> listNotifications(Long userId);

    UserNotification saveNotification(Long userId, Map<String, Object> data);

    void markNotificationRead(Long userId, Long id);

    void markAllNotificationsRead(Long userId);

    /** 个人数据数量统计。 */
    record UserDataStats(long policyFavorites, long careerRecords, long bpDrafts,
                         long notifications, long unreadNotifications) {
    }
}
