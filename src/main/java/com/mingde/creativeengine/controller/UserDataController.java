package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
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

import java.util.List;
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
    public Result<Map<String, Object>> stats() {
        // 登录态已由 AuthInterceptor 统一校验，这里直接取
        return Result.success(UserDataViews.stats(userDataService.stats(UserContext.getUserId())));
    }

    @GetMapping("/bp-drafts")
    public Result<List<Map<String, Object>>> listBpDrafts() {
        return Result.success(UserDataViews.bpDrafts(userDataService.listBpDrafts(UserContext.getUserId())));
    }

    @PostMapping("/bp-drafts")
    public Result<Map<String, Object>> saveBpDraft(@RequestBody Map<String, Object> data) {
        return Result.success(UserDataViews.bpDraft(userDataService.saveBpDraft(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/bp-drafts/{draftKey}")
    public Result<Boolean> deleteBpDraft(@PathVariable String draftKey) {
        userDataService.deleteBpDraft(UserContext.getUserId(), draftKey);
        return Result.success(true);
    }

    @GetMapping("/career-records")
    public Result<List<Map<String, Object>>> listCareerRecords() {
        return Result.success(UserDataViews.careerRecords(
                userDataService.listCareerRecords(UserContext.getUserId())));
    }

    @PostMapping("/career-records")
    public Result<Map<String, Object>> saveCareerRecord(@RequestBody Map<String, Object> data) {
        return Result.success(UserDataViews.careerRecord(
                userDataService.saveCareerRecord(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/career-records/{recordKey}")
    public Result<Boolean> deleteCareerRecord(@PathVariable String recordKey) {
        userDataService.deleteCareerRecord(UserContext.getUserId(), recordKey);
        return Result.success(true);
    }

    @GetMapping("/policy-favorites")
    public Result<List<Map<String, Object>>> listPolicyFavorites() {
        return Result.success(UserDataViews.policyFavorites(
                userDataService.listPolicyFavorites(UserContext.getUserId())));
    }

    @PostMapping("/policy-favorites")
    public Result<Map<String, Object>> savePolicyFavorite(@RequestBody Map<String, Object> data) {
        return Result.success(UserDataViews.policyFavorite(
                userDataService.savePolicyFavorite(UserContext.getUserId(), data)));
    }

    @DeleteMapping("/policy-favorites")
    public Result<Boolean> deletePolicyFavorite(@RequestParam String key) {
        userDataService.deletePolicyFavorite(UserContext.getUserId(), key);
        return Result.success(true);
    }

    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> listNotifications() {
        return Result.success(UserDataViews.notifications(
                userDataService.listNotifications(UserContext.getUserId())));
    }

    @PostMapping("/notifications")
    public Result<Map<String, Object>> saveNotification(@RequestBody Map<String, Object> data) {
        return Result.success(UserDataViews.notification(
                userDataService.saveNotification(UserContext.getUserId(), data)));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Boolean> markNotificationRead(@PathVariable Long id) {
        userDataService.markNotificationRead(UserContext.getUserId(), id);
        return Result.success(true);
    }

    @PutMapping("/notifications/read-all")
    public Result<Boolean> markAllNotificationsRead() {
        userDataService.markAllNotificationsRead(UserContext.getUserId());
        return Result.success(true);
    }
}
