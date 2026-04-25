package com.maghert.examcommon.pojo.query;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageQueryTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldExposeDefaultPaginationValues() {
        PageQuery pageQuery = new PageQuery();

        assertEquals(1L, pageQuery.getPageNum());
        assertEquals(10L, pageQuery.getPageSize());
        assertTrue(validator.validate(pageQuery).isEmpty());
    }

    @Test
    void shouldRejectOutOfRangePaginationValues() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(0L);
        pageQuery.setPageSize(101L);

        assertEquals(2, validator.validate(pageQuery).size());
    }
}
