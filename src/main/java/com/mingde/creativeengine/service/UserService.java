package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息（不含密码）
     */
    User login(String username, String password);
    
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @return 注册结果
     */
    User register(String username, String password, String nickname);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);
}
