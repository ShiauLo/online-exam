package com.maghert.examcommon.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应数据库表：sys_user
 */
@Data // Lombok注解：自动生成getter、setter、toString、equals、hashCode方法
@TableName("sys_user") // MyBatis-Plus注解：指定当前类对应的数据库表名
@Accessors(chain = true)
public class SysUser {

    /**
     * 用户ID
     * 对应数据库列：id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 登录用户名（唯一）
     * 对应数据库列：username (VARCHAR(50), NOT NULL, UNIQUE)
     */
    @NotBlank(message = "用户名不能为空")
    // JSR-303/JSR-380校验注解：用于在接口参数校验时，确保该字段不为null且不为空白字符串。
    // 这是一个很好的实践，可以在Controller层就拦截非法请求。
    @TableField("username")
    // MyBatis-Plus注解：标记为表字段。
    // 当属性名（username）和数据库列名（username）一致时，此注解可省略，但显式写出更清晰。
    private String username;

    /**
     * 加密存储的密码
     * 对应数据库列：password (VARCHAR(100), NOT NULL)
     */
    @NotBlank(message = "密码不能为空")
    // 同样使用校验注解，确保密码不为空。
    @TableField("password")
    private String password;

    /**
     * 用户真实姓名
     * 对应数据库列：real_name (VARCHAR(50), NOT NULL)
     */
    @NotBlank(message = "真实姓名不能为空")
    @TableField("real_name")
    private String realName;

    /**
     * 关联角色ID
     * 对应数据库列：role_id (INT, NOT NULL)
     */
    @TableField("role_id")
    private Integer roleId;

    /**
     * 账号状态
     * 对应数据库列：status (TINYINT, NOT NULL, DEFAULT 1)
     */
    @TableField("status")
    private Integer status;

    /**
     * 联系电话
     * 对应数据库列：phone (VARCHAR(20), DEFAULT NULL)
     */
    private String phoneNumber;

    /**
     * 邮箱
     * 对应数据库列：email (VARCHAR(100), DEFAULT NULL)
     */
    @Email(message = "请输入有效的邮箱地址")
    private String email;

    /**
     * 创建时间
     * 对应数据库列：create_time (DATETIME, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    // MyBatis-Plus注解：字段自动填充策略。
    // fill = FieldFill.INSERT：表示在执行插入（INSERT）操作时，MyBatis-Plus会自动填充此字段。
    // 这需要配合一个实现了 MetaObjectHandler 接口的自定义处理器来使用，在处理器中设置填充的值（通常是当前时间）。
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 对应数据库列：update_time (DATETIME, NOT NULL, ON UPDATE CURRENT_TIMESTAMP)
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    // fill = FieldFill.INSERT_UPDATE：表示在执行插入（INSERT）和更新（UPDATE）操作时，都自动填充此字段。
    private LocalDateTime updateTime;
}