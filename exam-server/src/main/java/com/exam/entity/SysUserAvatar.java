package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_avatar")
/** 账号头像，二进制存在 MySQL MEDIUMBLOB，和 sys_user 一对一。 */
public class SysUserAvatar {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String contentType;
    private byte[] photo;
    private LocalDateTime updatedAt;
}
