package com.maghert.examaccount;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component // 必须交给Spring管理
@Primary
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入操作时的填充逻辑
    @Override
    public void insertFill(MetaObject metaObject) {
        // 给create_time和update_time设置当前时间（插入时两者通常相同）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "operateTime", LocalDateTime.class, LocalDateTime.now());
    }

    // 更新操作时的填充逻辑
    @Override
    public void updateFill(MetaObject metaObject) {
        // 只更新update_time为当前时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
