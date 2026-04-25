package com.maghert.examquestion.context;

import com.maghert.examcommon.exception.BusinessException;

public interface QuestionAccessContext {

    Long requireUserId() throws BusinessException;

    Integer requireRoleId() throws BusinessException;

    String currentRequestId();
}
