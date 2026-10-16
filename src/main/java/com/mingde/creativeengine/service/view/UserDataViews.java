package com.mingde.creativeengine.service.view;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mingde.creativeengine.entity.UserBpDraft;
import com.mingde.creativeengine.entity.UserCareerRecord;
import com.mingde.creativeengine.entity.UserNotification;
import com.mingde.creativeengine.entity.UserPolicyFavorite;
import com.mingde.creativeengine.service.UserDataService.UserDataStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 个人数据的响应视图转换。
 *
 * <p><b>为什么用 {@code Map} 而不是 DTO</b>：政策收藏的响应是把「收藏当时的政策快照」
 * 字段平铺到顶层（形状随快照内容变化），固定字段的 DTO 表达不了。为了四种数据类型
 * 保持一致的写法就用 Map，代价是字段名写错要到运行时才发现，所以集中放这一个类里，
 * 改字段时只改一处。</p>
 */
public final class UserDataViews {

    private static final Gson GSON = new Gson();

    private UserDataViews() {
    }

    public static Map<String, Object> stats(UserDataStats stats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("policyFavorites", stats.policyFavorites());
        data.put("careerRecords", stats.careerRecords());
        data.put("bpDrafts", stats.bpDrafts());
        data.put("notifications", stats.notifications());
        data.put("unreadNotifications", stats.unreadNotifications());
        return data;
    }

    public static List<Map<String, Object>> bpDrafts(List<UserBpDraft> drafts) {
        return convert(drafts, UserDataViews::bpDraft);
    }

    public static Map<String, Object> bpDraft(UserBpDraft draft) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", draft.getDraftKey());
        data.put("projectName", draft.getProjectName());
        data.put("oneLiner", draft.getOneLiner());
        data.put("formData", parseJson(draft.getFormData(), new HashMap<>()));
        data.put("content", draft.getContent());
        data.put("createdAt", timeText(draft.getCreateTime()));
        data.put("updatedAt", timeText(draft.getUpdateTime()));
        return data;
    }

    public static List<Map<String, Object>> careerRecords(List<UserCareerRecord> records) {
        return convert(records, UserDataViews::careerRecord);
    }

    public static Map<String, Object> careerRecord(UserCareerRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.getRecordKey());
        data.put("createdAt", timeText(record.getCreateTime()));
        data.put("updatedAt", timeText(record.getUpdateTime()));
        data.put("results", parseJson(record.getResultData(), new ArrayList<>()));
        return data;
    }

    public static List<Map<String, Object>> policyFavorites(List<UserPolicyFavorite> favorites) {
        return convert(favorites, UserDataViews::policyFavorite);
    }

    public static Map<String, Object> policyFavorite(UserPolicyFavorite favorite) {
        Map<String, Object> data = new LinkedHashMap<>();
        Object snapshot = parseJson(favorite.getPolicySnapshot(), new HashMap<>());
        if (snapshot instanceof Map<?, ?> snapshotMap) {
            snapshotMap.forEach((key, value) -> data.put(String.valueOf(key), value));
        }
        data.put("favoriteId", favorite.getId());
        if (favorite.getPolicyId() != null) {
            data.put("id", favorite.getPolicyId());
        }
        data.put("policyName", favorite.getPolicyName());
        data.put("savedAt", timeText(favorite.getCreateTime()));
        return data;
    }

    public static List<Map<String, Object>> notifications(List<UserNotification> notifications) {
        return convert(notifications, UserDataViews::notification);
    }

    public static Map<String, Object> notification(UserNotification notification) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", notification.getId());
        data.put("title", notification.getTitle());
        data.put("type", notification.getType());
        data.put("content", notification.getContent());
        data.put("read", notification.getReadStatus() != null && notification.getReadStatus() == 1);
        data.put("createdAt", timeText(notification.getCreateTime()));
        return data;
    }

    private static <T> List<Map<String, Object>> convert(List<T> source, Function<T, Map<String, Object>> mapper) {
        List<Map<String, Object>> rows = new ArrayList<>(source.size());
        for (T item : source) {
            rows.add(mapper.apply(item));
        }
        return rows;
    }

    private static Object parseJson(String json, Object fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return GSON.fromJson(json, Object.class);
        } catch (JsonSyntaxException e) {
            return fallback;
        }
    }

    private static String timeText(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }
}
