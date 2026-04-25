package com.maghert.examcommon.apitest.collector;

import jakarta.validation.Valid;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReflectionControllerApiMetadataCollector implements ApiMetadataCollector {

    private final List<Class<?>> controllerTypes;

    private final String basePackage;

    public ReflectionControllerApiMetadataCollector(List<Class<?>> controllerTypes, String basePackage) {
        this.controllerTypes = controllerTypes;
        this.basePackage = basePackage;
    }

    @Override
    public List<ApiEndpointMetadata> collect() {
        List<ApiEndpointMetadata> endpoints = new ArrayList<>();
        for (Class<?> controllerType : controllerTypes) {
            if (!controllerType.isAnnotationPresent(RestController.class)) {
                continue;
            }
            if (StringUtils.hasText(basePackage) && !controllerType.getPackageName().startsWith(basePackage)) {
                continue;
            }
            String basePath = "";
            RequestMapping classMapping = controllerType.getAnnotation(RequestMapping.class);
            if (classMapping != null) {
                basePath = firstNonBlank(classMapping.path(), classMapping.value());
            }
            for (Method method : controllerType.getDeclaredMethods()) {
                MethodMapping methodMapping = resolveMethodMapping(method);
                if (methodMapping == null) {
                    continue;
                }
                ApiEndpointMetadata metadata = new ApiEndpointMetadata();
                metadata.setHandler(controllerType.getSimpleName() + "#" + method.getName());
                metadata.setHttpMethod(methodMapping.httpMethod());
                metadata.setPath(normalizePath(basePath, methodMapping.path()));
                RequestBodyBinding binding = resolveRequestBodyBinding(method);
                metadata.setRequestBodyType(binding.requestBodyType());
                metadata.setValidationBound(binding.validationBound());
                endpoints.add(metadata);
            }
        }
        endpoints.sort(Comparator.comparing(ApiEndpointMetadata::getPath).thenComparing(ApiEndpointMetadata::getHttpMethod));
        return endpoints;
    }

    private MethodMapping resolveMethodMapping(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return new MethodMapping(HttpMethod.GET.name(), firstNonBlank(mapping.path(), mapping.value()));
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            return new MethodMapping(HttpMethod.POST.name(), firstNonBlank(mapping.path(), mapping.value()));
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            return new MethodMapping(HttpMethod.PUT.name(), firstNonBlank(mapping.path(), mapping.value()));
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            return new MethodMapping(HttpMethod.DELETE.name(), firstNonBlank(mapping.path(), mapping.value()));
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            String httpMethod = mapping.method().length == 0 ? HttpMethod.GET.name() : mapping.method()[0].name();
            return new MethodMapping(httpMethod, firstNonBlank(mapping.path(), mapping.value()));
        }
        return null;
    }

    private RequestBodyBinding resolveRequestBodyBinding(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                boolean validationBound = parameter.isAnnotationPresent(Valid.class)
                        || parameter.isAnnotationPresent(Validated.class);
                return new RequestBodyBinding(parameter.getType(), validationBound);
            }
        }
        return new RequestBodyBinding(null, false);
    }

    private String firstNonBlank(String[] primary, String[] fallback) {
        for (String value : primary) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        for (String value : fallback) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String normalizePath(String basePath, String methodPath) {
        String combined = (StringUtils.hasText(basePath) ? basePath : "") + "/" + (StringUtils.hasText(methodPath) ? methodPath : "");
        String normalized = combined.replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private record MethodMapping(String httpMethod, String path) {
    }

    private record RequestBodyBinding(Class<?> requestBodyType, boolean validationBound) {
    }
}

