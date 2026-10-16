package com.mingde.creativeengine.security;

import java.time.Duration;

/**
 * 登录失败计数，用于挡住暴力破解。
 *
 * <p>原本登录接口对失败不做任何计数，密码可以无限试。现在连续失败达到阈值后
 * 在窗口期内直接拒绝，登录成功则清零。</p>
 *
 * <p><b>已知取舍</b>：按用户名计数意味着攻击者可以故意用错密码把某个账号"锁死"。
 * 生产上通常再叠加 IP 维度、验证码或设备指纹来缓解；这里保持最小实现，
 * 因为真正的兜底应该是登录风控而不是单点计数。</p>
 */
public interface LoginAttemptGuard {

    /** 该账号当前是否处于锁定窗口内。 */
    boolean isBlocked(String username);

    /** 记一次失败。窗口从最后一次失败重新起算。 */
    void recordFailure(String username);

    /** 登录成功后清空计数。 */
    void reset(String username);

    /** 本地实现的兜底容量，防止异常账号名把内存吃满。 */
    int MAX_TRACKED_ACCOUNTS = 10_000;

    /** 计数窗口。 */
    Duration window();

    /** 连续失败多少次后锁定。 */
    int maxAttempts();
}
