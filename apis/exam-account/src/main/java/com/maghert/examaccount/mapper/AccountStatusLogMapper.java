package com.maghert.examaccount.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maghert.examcommon.pojo.entity.SysAccountStatusLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountStatusLogMapper  extends BaseMapper<SysAccountStatusLog> {
}
