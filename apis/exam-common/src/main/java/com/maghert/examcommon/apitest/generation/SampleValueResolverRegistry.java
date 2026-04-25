package com.maghert.examcommon.apitest.generation;

import com.maghert.examcommon.apitest.model.ApiConstraintSpec;
import com.maghert.examcommon.apitest.model.ApiFieldSpec;

import java.util.List;
import java.util.Locale;

public class SampleValueResolverRegistry {

    private final List<FieldValueResolver> resolvers = List.of(
            new PhoneValueResolver(),
            new EmailValueResolver(),
            new LoginTypeValueResolver(),
            new PasswordValueResolver(),
            new TokenValueResolver(),
            new NumericValueResolver(),
            new BooleanValueResolver(),
            new ListValueResolver(),
            new StringFallbackValueResolver()
    );

    public Object resolveValidValue(ApiFieldSpec field) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(field))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No resolver for field " + field.getName()))
                .resolveValid(field);
    }

    public Object resolveInvalidValue(ApiFieldSpec field, ApiConstraintSpec constraint) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(field))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No resolver for field " + field.getName()))
                .resolveInvalid(field, constraint);
    }

    private interface FieldValueResolver {
        boolean supports(ApiFieldSpec field);
        Object resolveValid(ApiFieldSpec field);
        Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint);
    }

    private static final class PhoneValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return field.getSemanticHints().contains("PHONE_NUMBER") || matchesPattern(field, "^1[3-9]\\d{9}$");
        }
        public Object resolveValid(ApiFieldSpec field) {
            return "13800138000";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return "123456";
        }
    }

    private static final class EmailValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return field.getSemanticHints().contains("EMAIL") || field.hasConstraint("EMAIL");
        }
        public Object resolveValid(ApiFieldSpec field) {
            return "user@example.com";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return "invalid-email";
        }
    }

    private static final class LoginTypeValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return field.getSemanticHints().contains("LOGIN_TYPE");
        }
        public Object resolveValid(ApiFieldSpec field) {
            return "password_login";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return "unsupported_login_type";
        }
    }

    private static final class PasswordValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return field.getSemanticHints().contains("PASSWORD");
        }
        public Object resolveValid(ApiFieldSpec field) {
            return "Passw0rd!";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return "";
        }
    }

    private static final class TokenValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return field.getSemanticHints().contains("TOKEN");
        }
        public Object resolveValid(ApiFieldSpec field) {
            return "mock-token";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return "";
        }
    }

    private static final class NumericValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return "java.lang.Long".equals(field.getType())
                    || "long".equals(field.getType())
                    || "java.lang.Integer".equals(field.getType())
                    || "int".equals(field.getType());
        }
        public Object resolveValid(ApiFieldSpec field) {
            ApiConstraintSpec min = field.findConstraint("MIN");
            ApiConstraintSpec max = field.findConstraint("MAX");
            long candidate = min != null ? ((Number) min.attribute("value")).longValue() : 1L;
            if (max != null && candidate > ((Number) max.attribute("value")).longValue()) {
                candidate = ((Number) max.attribute("value")).longValue();
            }
            return "java.lang.Integer".equals(field.getType()) || "int".equals(field.getType()) ? (int) candidate : candidate;
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            long value = ((Number) constraint.attribute("value")).longValue();
            long candidate = "MIN".equals(constraint.getType()) ? value - 1 : value + 1;
            return "java.lang.Integer".equals(field.getType()) || "int".equals(field.getType()) ? (int) candidate : candidate;
        }
    }

    private static final class BooleanValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return "java.lang.Boolean".equals(field.getType()) || "boolean".equals(field.getType());
        }
        public Object resolveValid(ApiFieldSpec field) {
            return Boolean.TRUE;
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return Boolean.FALSE;
        }
    }

    private static final class ListValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return "java.util.List".equals(field.getType());
        }
        public Object resolveValid(ApiFieldSpec field) {
            if ("options".equalsIgnoreCase(field.getName())) {
                return List.of("Option A", "Option B", "Option C", "Option D");
            }
            return List.of("sample");
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            return List.of();
        }
    }

    private static final class StringFallbackValueResolver implements FieldValueResolver {
        public boolean supports(ApiFieldSpec field) {
            return "java.lang.String".equals(field.getType());
        }
        public Object resolveValid(ApiFieldSpec field) {
            return field.getName().toLowerCase(Locale.ROOT) + "-sample";
        }
        public Object resolveInvalid(ApiFieldSpec field, ApiConstraintSpec constraint) {
            if ("NOT_BLANK".equals(constraint.getType())) {
                return "";
            }
            if ("PATTERN".equals(constraint.getType())) {
                return "invalid";
            }
            return null;
        }
    }

    private static boolean matchesPattern(ApiFieldSpec field, String regexp) {
        ApiConstraintSpec pattern = field.findConstraint("PATTERN");
        return pattern != null && regexp.equals(pattern.attribute("regexp"));
    }
}

