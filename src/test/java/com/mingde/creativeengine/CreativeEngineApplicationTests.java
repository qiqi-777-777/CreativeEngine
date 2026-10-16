package com.mingde.creativeengine;

import com.mingde.creativeengine.aop.AgentCallLogAspect;
import com.mingde.creativeengine.controller.AgentController;
import com.mingde.creativeengine.mq.AgentLogPublisher;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CreativeEngineApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    /**
     * {@code @Transactional} 依赖事务管理器：少了它，注解写了也不生效，而且要到运行期调用时才报错。
     */
    @Test
    void transactionManagerIsAvailable() {
        assertNotNull(context.getBean(PlatformTransactionManager.class),
                "缺少事务管理器时 @Transactional 会在运行期才报错");
    }

    /**
     * 切面最容易「静默失效」：少了 aspectjweaver、切点写错、Bean 没被扫到，
     * 应用照样能正常启动，只是日志悄悄不再写了。这里把装配关系固定住。
     */
    @Test
    void agentControllerIsProxiedByCallLogAspect() {
        assertNotNull(context.getBean(AgentLogPublisher.class), "日志投递入口应已装配");
        assertEquals(1, context.getBeansOfType(AgentCallLogAspect.class).size(), "应存在唯一的 Agent 日志切面");

        AgentController controller = context.getBean(AgentController.class);
        assertTrue(AopUtils.isAopProxy(controller), "AgentController 应被代理，否则 @AgentCallLog 不会生效");
    }
}
