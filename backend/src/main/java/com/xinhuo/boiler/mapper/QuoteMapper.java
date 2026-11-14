package com.xinhuo.boiler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinhuo.boiler.entity.Quote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuoteMapper extends BaseMapper<Quote> {
}

