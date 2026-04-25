package com.maghert.examcommon.utils;

import com.maghert.examcommon.constants.AuthConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestIdUtilsTests {

    @Test
    void shouldPreferInternalRequestIdHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AuthConstants.INTERNAL_REQUEST_ID_HEADER, "internal-req");
        headers.add(AuthConstants.REQUEST_ID_HEADER, "external-req");

        assertEquals("internal-req", RequestIdUtils.resolveOrGenerate(headers));
    }

    @Test
    void shouldFallbackToExternalRequestIdHeaderThenGenerate() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AuthConstants.REQUEST_ID_HEADER, "external-req");

        assertEquals("external-req", RequestIdUtils.resolveOrGenerate(headers));
        assertFalse(RequestIdUtils.resolveOrGenerate(new HttpHeaders()).isBlank());
    }
}
