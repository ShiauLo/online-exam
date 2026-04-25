package com.maghert.examcommon.apitest.generation;

import com.maghert.examcommon.apitest.model.ApiConstraintSpec;
import com.maghert.examcommon.apitest.model.ApiFieldSpec;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ConstraintIntrospector {

    public List<ApiFieldSpec> describeFields(Class<?> requestBodyType) {
        if (requestBodyType == null) {
            return List.of();
        }
        List<ApiFieldSpec> fields = new ArrayList<>();
        for (Field field : requestBodyType.getDeclaredFields()) {
            ApiFieldSpec fieldSpec = new ApiFieldSpec();
            fieldSpec.setName(field.getName());
            fieldSpec.setType(field.getType().getName());
            fieldSpec.setSemanticHints(detectSemanticHints(field));
            fieldSpec.setConstraints(extractConstraints(field));
            fields.add(fieldSpec);
        }
        return fields;
    }

    private List<String> detectSemanticHints(Field field) {
        String normalized = field.getName().toLowerCase(Locale.ROOT);
        List<String> hints = new ArrayList<>();
        if (normalized.contains("phone")) {
            hints.add("PHONE_NUMBER");
        }
        if (normalized.contains("email")) {
            hints.add("EMAIL");
        }
        if (normalized.contains("password")) {
            hints.add("PASSWORD");
        }
        if (normalized.contains("token")) {
            hints.add("TOKEN");
        }
        if (normalized.contains("logintype")) {
            hints.add("LOGIN_TYPE");
        }
        return hints;
    }

    private List<ApiConstraintSpec> extractConstraints(Field field) {
        List<ApiConstraintSpec> constraints = new ArrayList<>();
        NotNull notNull = field.getAnnotation(NotNull.class);
        if (notNull != null && isDefaultConstraint(notNull)) {
            constraints.add(new ApiConstraintSpec("NOT_NULL"));
        }
        NotBlank notBlank = field.getAnnotation(NotBlank.class);
        if (notBlank != null && isDefaultConstraint(notBlank)) {
            constraints.add(new ApiConstraintSpec("NOT_BLANK"));
        }
        Email email = field.getAnnotation(Email.class);
        if (email != null && isDefaultConstraint(email)) {
            constraints.add(new ApiConstraintSpec("EMAIL"));
        }
        Pattern pattern = field.getAnnotation(Pattern.class);
        if (pattern != null && isDefaultConstraint(pattern)) {
            ApiConstraintSpec spec = new ApiConstraintSpec("PATTERN");
            spec.getAttributes().put("regexp", pattern.regexp());
            constraints.add(spec);
        }
        Min min = field.getAnnotation(Min.class);
        if (min != null && isDefaultConstraint(min)) {
            ApiConstraintSpec spec = new ApiConstraintSpec("MIN");
            spec.getAttributes().put("value", min.value());
            constraints.add(spec);
        }
        Max max = field.getAnnotation(Max.class);
        if (max != null && isDefaultConstraint(max)) {
            ApiConstraintSpec spec = new ApiConstraintSpec("MAX");
            spec.getAttributes().put("value", max.value());
            constraints.add(spec);
        }
        return constraints;
    }

    private boolean isDefaultConstraint(Annotation annotation) {
        try {
            Method groupsMethod = annotation.annotationType().getMethod("groups");
            Class<?>[] groups = (Class<?>[]) groupsMethod.invoke(annotation);
            return groups.length == 0 || Arrays.asList(groups).contains(Default.class);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}

