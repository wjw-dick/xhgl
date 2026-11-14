package com.xinhuo.boiler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {
    private String originalName;
    private String storedName;
    private String url;
    private String storagePath;
    private Long size;
    private String contentType;
    private String category;
    private String driver;
}
