package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("solution")
public class Solution extends BaseEntity {
    private String solutionNo;
    private Long demandId;
    private String name;
    private Integer version;
    private Long productId;
    private String productName;
    private String content;
    private String attachments;
    private String status;
    private String shareToken;
    private Integer viewCount;
    private LocalDateTime confirmTime;
    private Long createdBy;
}
