package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("installation")
public class Installation extends BaseEntity {
    private Long contractId;
    private String stage;
    private LocalDate planDate;
    private LocalDate actualDate;
    private String photos;
    private String acceptanceUrl;
    private String acceptanceSigner;
    private String remark;
    private String status;
}
