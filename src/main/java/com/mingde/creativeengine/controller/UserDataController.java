package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.security.UserContext;
import com.mingde.creativeengine.service.UserDataService;
import com.mingde.creativeengine.service.view.UserDataViews;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户个人数据接口：BP 草稿、职业匹配记录、政策收藏、通知。
 *
 * <p>这里只做三件事：取登录用户、调服务、套响应结构。
 * 数据的查询与「先查后写」的并发互斥都在 {@link UserDataService} 里。</p>
 */
@Tag(name = "用户数据", description = "BP 草稿、职业匹配记录、政策收藏与通知")
@RestController
@RequestMapping("/api/user-data")
public class UserDataController {

    private final UserDataService userDataService;

    public UserDataController(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        // 登录态已由 AuthInterceptor 统一校验，这里直接取
        return success(UserDataViews.stats(userDataService.stats(UserContext.getUserId())));
    }

    @GetMapping("/bp-drafts")
    public Map<String, Object> listBpDrafts() {
        return success(UserDataViews.bpDrafts(userDataService.listBpDrafts(UserContext.getUserId())));
    }

    @PostMapping("/bp-drafts")
    public Map<String, Object> saveBpDraft(@RequestBody Map<String, Object> data) {
        return success(UserDataViews.bpDraft(userDataService.saveBpDraft(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/bp-drafts/{draftKey}")
    public Map<String, Object> deleteBpDraft(@PathVariable String draftKey) {
        userDataService.deleteBpDraft(UserContext.getUserId(), draftKey);
        return success(true);
    }

    @GetMapping("/career-records")
    public Map<String, Object> listCareerRecords() {
        return success(UserDataViews.careerRecords(userDataService.listCareerRecords(UserContext.getUserId())));
    }

    @PostMapping("/career-records")
    public Map<String, Object> saveCareerRecord(@RequestBody Map<String, Object> data) {
        return success(UserDataViews.careerRecord(userDataService.saveCareerRecord(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/career-records/{recordKey}")
    public Map<String, Object> deleteCareerRecord(@PathVariable String recordKey) {
        userDataService.deleteCareerRecord(UserContext.getUserId(), recordKey);
        return success(true);
    }

    @GetMapping("/policy-favorites")
    public Map<String, Object> listPolicyFavorites() {
        return success(UserDataViews.policyFavorites(userDataService.listPolicyFavorites(UserContext.getUserId())));
    }

    @PostMapping("/policy-favorites")
    public Map<String, Object> savePolicyFavorite(@RequestBody Map<String, Object> data) {
        return success(UserDataViews.policyFavorite(
                userDataService.savePolicyFavorite(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/policy-favorites")
    public Map<String, Object> deletePolicyFavorite(@RequestParam String key) {
        userDataService.deletePolicyFavorite(UserContext.getUserId(), key);
        return success(true);
    }

    @GetMapping("/notifications")
    public Map<String, Object> listNotifications() {
        return success(UserDataViews.notifications(userDataService.listNotifications(UserContext.getUserId())));
    }

    @PostMapping("/notifications")
    public Map<String, Object> saveNotification(@RequestBody Map<String, Object> data) {
        return success(UserDataViews.notification(userDataService.saveNotification(UserContext.getUserId(), data)));
    }

    @PutMapping("/notifications/{id}/read")
    public Map<String, Object> markNotificationRead(@PathVariable Long id) {
        userDataService.markNotificationRead(UserContext.getUserId(), id);
        return success(true);
    }

    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsRead() {
        userDataService.markAllNotificationsRead(UserContext.getUserId());
        return success(true);
    }

    /**
     * 本控制器的成功码是 200，而 {@code Result.success()} 用的是 0（ResultCode.SUCCESS），
     * 两者在项目里并存。小程序按 200 判断这个模块，所以这里保持原样；
     * 统一响应码是另一件事，需要前后端一起改，不能在后端悄悄换掉。
     */
    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}
