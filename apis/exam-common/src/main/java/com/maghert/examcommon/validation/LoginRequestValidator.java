package com.maghert.examcommon.validation;

import com.maghert.examcommon.pojo.dto.LoginDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LoginRequestValidator implements ConstraintValidator<ValidLoginRequest, LoginDTO> {

    private static final String PASSWORD_LOGIN = "password_login";
    private static final String ONE_KEY_LOGIN = "one_key_login";

    @Override
    public boolean isValid(LoginDTO value, ConstraintValidatorContext context) {
        if (value == null || isBlank(value.getLoginType())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        return switch (value.getLoginType()) {
            case PASSWORD_LOGIN -> validatePasswordLogin(value, context);
            case ONE_KEY_LOGIN -> validateOneKeyLogin(value, context);
            default -> addViolation(context, "unsupported loginType", "loginType");
        };
    }

    private boolean validatePasswordLogin(LoginDTO value, ConstraintValidatorContext context) {
        boolean valid = true;
        if (isBlank(value.getAccount())) {
            valid = addViolation(context, "account is required for password_login", "account") && valid;
        }
        if (isBlank(value.getPassword())) {
            valid = addViolation(context, "password is required for password_login", "password") && valid;
        }
        return valid;
    }

    private boolean validateOneKeyLogin(LoginDTO value, ConstraintValidatorContext context) {
        boolean valid = true;
        if (isBlank(value.getPhone())) {
            valid = addViolation(context, "phone is required for one_key_login", "phone") && valid;
        }
        if (isBlank(value.getVerifyCode())) {
            valid = addViolation(context, "verifyCode is required for one_key_login", "verifyCode") && valid;
        }
        return valid;
    }

    private boolean addViolation(ConstraintValidatorContext context, String message, String field) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
