package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_record")
public class TicketRecord extends BaseEntity {
    private Long ticketId;
    private String operator;
    private String action;
    private String content;
    private String photos;
    private String partsJson;
}
