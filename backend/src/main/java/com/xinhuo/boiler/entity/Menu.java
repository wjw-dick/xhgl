package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu")
public class Menu extends BaseEntity {
    private String target;
    private Long parentId;
    private String title;
    private String icon;
    private String path;
    private String menuType;
    private String externalUrl;
    private Integer sort;
    private Integer visible;
}
