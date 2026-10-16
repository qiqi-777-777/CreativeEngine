package com.mingde.creativeengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理接口的角色校验。
 *
 * <p>只负责「已登录用户里哪些人算管理员」。是否登录由 {@link AuthInterceptor} 负责，
 * 因此本拦截器必须排在它之后注册（见 WebConfig），否则 UserContext 还是空的。</p>
 *
 * <p><b>为什么按路径而不是按方法上的注解</b>：注解方案漏标一个方法就等于放行，是「默认允许」；
 * 路径规则是「命中即校验」，新增接口只要落在这些前缀下就自动受保护。代价是新增管理路径要同步
 * 更新 {@code WebConfig.ADMIN_PATHS}，这一点由 {@code AdminPathCoverageTest} 扫描全部 Controller 兜底。</p>
 *
 * <p>角色是每次请求查库拿的。管理接口调用频率极低，先不引入缓存，
 * 换来的是「改了角色立刻生效」；如果以后管理接口变热，再把角色放进入登录态或加一层缓存。</p>
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminInterceptor.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AdminInterceptor(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = UserContext.getUserId();
        User user = userId == null ? null : userService.getUserById(userId);

        if (user == null || !UserRole.of(user.getRole()).isAdmin()) {
            log.warn("非管理员访问管理接口：userId={}, {} {}", userId, request.getMethod(), request.getRequestURI());
            ApiErrorResponse.write(response, objectMapper,
                    HttpStatus.FORBIDDEN.value(), Result.error(ResultCode.PERMISSION_DENIED));
            return false;
        }
        return true;
    }
}
