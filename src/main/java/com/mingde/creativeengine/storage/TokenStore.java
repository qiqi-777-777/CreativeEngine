package com.mingde.creativeengine.storage;

/**
 * 登录 token 存储。
 *
 * <p>改造前 token 存在 {@code LoginController} 的静态 {@code ConcurrentHashMap} 里，
 * 有三个问题：进程重启即失效、多实例无法共享、没有过期时间也没有回收。
 * 抽象成接口后，单机默认走内存实现，开启 Redis 后自动切换，业务代码无需改动。</p>
 */
public interface TokenStore {

    /**
     * 保存 token 与用户的映射（Redis 实现会带上过期时间）。
     */
    void save(String token, Long userId);

    /**
     * 解析 token 对应的用户 ID，token 非法或不存在时返回 {@code null}。
     *
     * <p>入参允许带 {@code Bearer } 前缀，内部统一剥离。</p>
     */
    Long resolve(String token);

    /**
     * 主动失效某个 token（退出登录时调用）。
     */
    void revoke(String token);
}
