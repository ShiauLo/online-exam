package com.maghert.examcommon.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号封禁/解封操作日志实体类
 * 对应数据库表：sys_account_status_log
 *
 * @author 你的名称
 * @date 2026-01-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_account_status_log") // 指定数据库表名（MyBatis-Plus 注解）
public class SysAccountStatusLog {

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID) // 雪花算法主键策略
    private Long id;

    /**
     * 操作人ID（管理员/运营账号ID）
     */
    private Long operateUserId;

    /**
     * 操作人账号/姓名（冗余存储，避免操作人账号删除后无记录）
     */
    private String operateUserName;

    /**
     * 操作人角色（ADMIN/TEACHER/OPERATOR）
     */
    private String operateUserRole;

    /**
     * 被操作的账号ID
     */
    private Long targetUserId;

    /**
     * 被操作账号的手机号（冗余存储，建议脱敏后存储，如138****8000）
     */
    private String targetUserPhone;

    /**
     * 操作类型：0=解封，1=封禁
     * 建议用枚举：0-UNBAN(解封)，1-BAN(封禁)
     */
    private Integer operateType;

    /**
     * 操作原因（必填，如"违规答题"/"误封解封"/"超期封禁自动解封"）
     */
    private String operateReason;

    /**
     * 操作前账号状态：0=正常，1=封禁
     */
    private Integer beforeStatus;

    /**
     * 操作后账号状态：0=正常，1=封禁
     */
    private Integer afterStatus;

    /**
     * 操作IP地址（客户端IP）
     */
    @TableField("operate_ip")
    private String operateIp;

    /**
     * 操作时间（默认当前时间）
     */
    @TableField(value = "operate_time", fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime operateTime;


}