package com.xinhuo.boiler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinhuo.boiler.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}

