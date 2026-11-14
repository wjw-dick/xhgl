package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("follow_record")
public class FollowRecord extends BaseEntity {
    private Long demandId;
    private Long userId;
    private String userName;
    private LocalDateTime followTime;
    private String method;
    private String content;
    private LocalDateTime nextTime;
}
