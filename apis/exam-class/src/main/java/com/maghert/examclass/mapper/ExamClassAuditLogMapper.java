package com.maghert.examclass.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maghert.examclass.model.ExamClassAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamClassAuditLogMapper extends BaseMapper<ExamClassAuditLogEntity> {
}
