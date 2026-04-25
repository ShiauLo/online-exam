package com.maghert.examaccount.strategy.strategyFactory;

import com.maghert.examcommon.exception.NoSuchStrategyException;
import com.maghert.examaccount.strategy.LoginStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录策略工厂（Spring容器管理，自动扫描所有LoginStrategy实现类）
 */
@Component
public class LoginStrategyFactory {
    /**
     * 缓存策略实例：key=登录类型code，value=策略实例
     */
    private final Map<String, LoginStrategy> strategyMap = new HashMap<>();

    /**
     * 利用Spring自动注入所有LoginStrategy实现类，初始化策略缓存
     */
    public LoginStrategyFactory(List<LoginStrategy> strategyList) {
        for (LoginStrategy strategy : strategyList) {
            strategyMap.put(strategy.supportType().getCode(), strategy);
        }
    }

    /**
     * 根据登录类型获取策略实例
     */
    public LoginStrategy getStrategy(String loginTypeCode) throws NoSuchStrategyException {
        LoginStrategy strategy = strategyMap.get(loginTypeCode);
        if (strategy == null) {
            throw new NoSuchStrategyException("不支持的登录类型：" + loginTypeCode);
        }
        return strategy;
    }
}