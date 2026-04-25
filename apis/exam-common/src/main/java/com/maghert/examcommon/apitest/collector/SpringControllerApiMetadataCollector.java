package com.maghert.examcommon.apitest.collector;

import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class SpringControllerApiMetadataCollector implements ApiMetadataCollector {

    private final RequestMappingHandlerMapping handlerMapping;

    private final String basePackage;

    public SpringControllerApiMetadataCollector(RequestMappingHandlerMapping handlerMapping, String basePackage) {
        this.handlerMapping = handlerMapping;
        this.basePackage = basePackage;
    }

    @Override
    public List<ApiEndpointMetadata> collect() {
        if (handlerMapping == null) {
            return List.of();
        }
        List<ApiEndpointMetadata> endpoints = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            Class<?> controllerType = handlerMethod.getBeanType();
            if (!controllerType.isAnnotationPresent(RestController.class)) {
                continue;
            }
            if (StringUtils.hasText(basePackage) && !controllerType.getPackageName().startsWith(basePackage)) {
                continue;
            }
            Set<String> paths = new TreeSet<>();
            var pathPatternsCondition = mappingInfo.getPathPatternsCondition();
            if (pathPatternsCondition != null) {
                pathPatternsCondition.getPatternValues().forEach(paths::add);
            }
            var patternsCondition = mappingInfo.getPatternsCondition();
            if (patternsCondition != null) {
                patternsCondition.getPatterns().forEach(paths::add);
            }
            String path = paths.isEmpty() ? "/" : paths.iterator().next();
            String httpMethod = mappingInfo.getMethodsCondition().getMethods().isEmpty()
                    ? "GET"
                    : Objects.requireNonNull(mappingInfo.getMethodsCondition().getMethods().iterator().next()).name();

            ApiEndpointMetadata metadata = new ApiEndpointMetadata();
            metadata.setHandler(controllerType.getSimpleName() + "#" + handlerMethod.getMethod().getName());
            metadata.setHttpMethod(httpMethod);
            metadata.setPath(path);
            RequestBodyBinding binding = resolveRequestBodyBinding(handlerMethod);
            metadata.setRequestBodyType(binding.requestBodyType());
            metadata.setValidationBound(binding.validationBound());
            endpoints.add(metadata);
        }
        endpoints.sort(java.util.Comparator.comparing(ApiEndpointMetadata::getPath).thenComparing(ApiEndpointMetadata::getHttpMethod));
        return endpoints;
    }

    private RequestBodyBinding resolveRequestBodyBinding(HandlerMethod handlerMethod) {
        for (Parameter parameter : handlerMethod.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                boolean validationBound = parameter.isAnnotationPresent(Valid.class)
                        || parameter.isAnnotationPresent(Validated.class);
                return new RequestBodyBinding(parameter.getType(), validationBound);
            }
        }
        return new RequestBodyBinding(null, false);
    }

    private record RequestBodyBinding(Class<?> requestBodyType, boolean validationBound) {
    }
}

