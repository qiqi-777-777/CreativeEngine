package com.mingde.creativeengine.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mingde.creativeengine.entity.UserBpDraft;
import com.mingde.creativeengine.entity.UserCareerRecord;
import com.mingde.creativeengine.entity.UserNotification;
import com.mingde.creativeengine.entity.UserPolicyFavorite;
import com.mingde.creativeengine.mapper.UserBpDraftMapper;
import com.mingde.creativeengine.mapper.UserCareerRecordMapper;
import com.mingde.creativeengine.mapper.UserNotificationMapper;
import com.mingde.creativeengine.mapper.UserPolicyFavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-data")
public class UserDataController {

    private static final Gson gson = new Gson();

    @Autowired
    private UserBpDraftMapper bpDraftMapper;

    @Autowired
    private UserCareerRecordMapper careerRecordMapper;

    @Autowired
    private UserPolicyFavoriteMapper policyFavoriteMapper;

    @Autowired
    private UserNotificationMapper notificationMapper;

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("policyFavorites", policyFavoriteMapper.selectCount(policyFavoriteQuery(userId)));
        data.put("careerRecords", careerRecordMapper.selectCount(careerRecordQuery(userId)));
        data.put("bpDrafts", bpDraftMapper.selectCount(bpDraftQuery(userId)));
        data.put("notifications", notificationMapper.selectCount(notificationQuery(userId)));
        data.put("unreadNotifications", notificationMapper.selectCount(
                notificationQuery(userId).eq("is_read", 0)
        ));
        return success(data);
    }

    @GetMapping("/bp-drafts")
    public Map<String, Object> listBpDrafts(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        List<Map<String, Object>> rows = bpDraftMapper.selectList(
                bpDraftQuery(userId).orderByDesc("update_time").orderByDesc("id")
        ).stream().map(this::toBpDraft).collect(Collectors.toList());
        return success(rows);
    }

    @PostMapping("/bp-drafts")
    public Map<String, Object> saveBpDraft(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> data) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        String draftKey = text(data.get("id"), "bp-" + System.currentTimeMillis());
        UserBpDraft draft = bpDraftMapper.selectOne(
                bpDraftQuery(userId).eq("draft_key", draftKey).last("LIMIT 1")
        );
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
        draft.setFormData(gson.toJson(formData == null ? new HashMap<>() : formData));
        draft.setContent(text(data.get("content"), ""));
        draft.setUpdateTime(now);

        if (isNew) {
            bpDraftMapper.insert(draft);
        } else {
            bpDraftMapper.updateById(draft);
        }

        return success(toBpDraft(draft));
    }

    @DeleteMapping("/bp-drafts/{draftKey}")
    public Map<String, Object> deleteBpDraft(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String draftKey) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        bpDraftMapper.delete(bpDraftQuery(userId).eq("draft_key", draftKey));
        return success(true);
    }

    @GetMapping("/career-records")
    public Map<String, Object> listCareerRecords(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        List<Map<String, Object>> rows = careerRecordMapper.selectList(
                careerRecordQuery(userId).orderByDesc("create_time").orderByDesc("id")
        ).stream().map(this::toCareerRecord).collect(Collectors.toList());
        return success(rows);
    }

    @PostMapping("/career-records")
    public Map<String, Object> saveCareerRecord(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> data) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        String recordKey = text(data.get("id"), "career-" + System.currentTimeMillis());
        UserCareerRecord record = careerRecordMapper.selectOne(
                careerRecordQuery(userId).eq("record_key", recordKey).last("LIMIT 1")
        );
        LocalDateTime now = LocalDateTime.now();
        boolean isNew = record == null;
        if (isNew) {
            record = new UserCareerRecord();
            record.setUserId(userId);
            record.setRecordKey(recordKey);
            record.setCreateTime(now);
        }

        Object results = data.get("results");
        record.setResultData(gson.toJson(results == null ? new ArrayList<>() : results));
        record.setUpdateTime(now);

        if (isNew) {
            careerRecordMapper.insert(record);
        } else {
            careerRecordMapper.updateById(record);
        }

        return success(toCareerRecord(record));
    }

    @DeleteMapping("/career-records/{recordKey}")
    public Map<String, Object> deleteCareerRecord(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String recordKey) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        careerRecordMapper.delete(careerRecordQuery(userId).eq("record_key", recordKey));
        return success(true);
    }

    @GetMapping("/policy-favorites")
    public Map<String, Object> listPolicyFavorites(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        List<Map<String, Object>> rows = policyFavoriteMapper.selectList(
                policyFavoriteQuery(userId).orderByDesc("create_time").orderByDesc("id")
        ).stream().map(this::toPolicyFavorite).collect(Collectors.toList());
        return success(rows);
    }

    @PostMapping("/policy-favorites")
    public Map<String, Object> savePolicyFavorite(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> data) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        String policyName = text(data.get("policyName"), "政策详情");
        UserPolicyFavorite favorite = policyFavoriteMapper.selectOne(
                policyFavoriteQuery(userId).eq("policy_name", policyName).last("LIMIT 1")
        );
        LocalDateTime now = LocalDateTime.now();
        boolean isNew = favorite == null;
        if (isNew) {
            favorite = new UserPolicyFavorite();
            favorite.setUserId(userId);
            favorite.setCreateTime(now);
        }

        favorite.setPolicyId(toLong(data.get("id")));
        favorite.setPolicyName(policyName);
        favorite.setPolicySnapshot(gson.toJson(data));

        if (isNew) {
            policyFavoriteMapper.insert(favorite);
        } else {
            policyFavoriteMapper.updateById(favorite);
        }

        return success(toPolicyFavorite(favorite));
    }

    @DeleteMapping("/policy-favorites")
    public Map<String, Object> deletePolicyFavorite(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam String key) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        QueryWrapper<UserPolicyFavorite> wrapper = policyFavoriteQuery(userId);
        wrapper.and((query) -> {
            query.eq("policy_name", key);
            Long policyId = toLong(key);
            if (policyId != null) {
                query.or().eq("policy_id", policyId);
            }
        });
        policyFavoriteMapper.delete(wrapper);
        return success(true);
    }

    @GetMapping("/notifications")
    public Map<String, Object> listNotifications(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        List<Map<String, Object>> rows = notificationMapper.selectList(
                notificationQuery(userId).orderByDesc("create_time").orderByDesc("id")
        ).stream().map(this::toNotification).collect(Collectors.toList());
        return success(rows);
    }

    @PostMapping("/notifications")
    public Map<String, Object> saveNotification(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> data) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(text(data.get("title"), "系统通知"));
        notification.setType(text(data.get("type"), "通知"));
        notification.setContent(text(data.get("content"), ""));
        notification.setReadStatus(Boolean.TRUE.equals(data.get("read")) ? 1 : 0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        return success(toNotification(notification));
    }

    @PutMapping("/notifications/{id}/read")
    public Map<String, Object> markNotificationRead(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        notificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .eq("user_id", userId)
                .eq("id", id)
                .set("is_read", 1));
        return success(true);
    }

    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsRead(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return unauthorized();
        }

        notificationMapper.update(null, new UpdateWrapper<UserNotification>()
                .eq("user_id", userId)
                .set("is_read", 1));
        return success(true);
    }

    private Long currentUserId(String token) {
        return LoginController.resolveUserId(token);
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

    private Map<String, Object> toBpDraft(UserBpDraft draft) {
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

    private Map<String, Object> toCareerRecord(UserCareerRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.getRecordKey());
        data.put("createdAt", timeText(record.getCreateTime()));
        data.put("updatedAt", timeText(record.getUpdateTime()));
        data.put("results", parseJson(record.getResultData(), new ArrayList<>()));
        return data;
    }

    private Map<String, Object> toPolicyFavorite(UserPolicyFavorite favorite) {
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

    private Map<String, Object> toNotification(UserNotification notification) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", notification.getId());
        data.put("title", notification.getTitle());
        data.put("type", notification.getType());
        data.put("content", notification.getContent());
        data.put("read", notification.getReadStatus() != null && notification.getReadStatus() == 1);
        data.put("createdAt", timeText(notification.getCreateTime()));
        return data;
    }

    private Object parseJson(String json, Object fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return gson.fromJson(json, Object.class);
        } catch (JsonSyntaxException e) {
            return fallback;
        }
    }

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

    private String timeText(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> unauthorized() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "未登录或登录已过期");
        return result;
    }
}
