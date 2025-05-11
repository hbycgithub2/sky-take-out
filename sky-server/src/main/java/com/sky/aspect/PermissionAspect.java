package com.sky.aspect;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Autowired
    private HttpServletRequest request; // 自动注入请求对象

    // 权限列表（可从数据库/缓存获取）
    private static final List<String> ALLOWED_PATHS = Arrays.asList(
            "/ts/search",
            "/other/allowed/path",
            "/admin/employee/{id}"
    );

    @Pointcut("execution(* com.sky.controller..*.*(..))&& @annotation(com.sky.annotation.RequirePermission))")
    public void permissionPointcut() {
    }

    @Around("permissionPointcut()")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取完整请求路径
        String fullPath = buildFullPath(joinPoint);


        // 2. 权限校验逻辑
        if (!ALLOWED_PATHS.contains(fullPath)) {
            System.out.println("权限不足，禁止访问1111");
            return Result.error("权限不足，禁止访问");
        }

        // 3. 放行请求
        return joinPoint.proceed();
    }

    /**
     * 构建完整请求路径
     */
    private String buildFullPath(ProceedingJoinPoint joinPoint) {
        // 获取类路径
        String classPath = getClassPath(joinPoint);
        // 获取方法路径
        String methodPath = getMethodPath(joinPoint);
        // 路径拼接标准化
        return normalizePath(classPath, methodPath);

    }


    /**
     * 获取类级别路径
     */
    private String getClassPath(ProceedingJoinPoint joinPoint) {
        RequestMapping classMapping = joinPoint.getTarget().getClass()
                .getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            return classMapping.value()[0].replaceAll("/+$", "");
        }
        return "";
    }

    /**
     * 获取方法级别路径（兼容多种注解）
     */
    private String getMethodPath(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // 优先级：GetMapping/PostMapping > RequestMapping
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null && getMapping.value().length > 0) {
            return getMapping.value()[0];
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null && postMapping.value().length > 0) {
            return postMapping.value()[0];
        }

        RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
        if (reqMapping != null && reqMapping.value().length > 0) {
            return reqMapping.value()[0];
        }

        return "";
    }

    /**
     * 标准化路径格式
     */
    private String normalizePath(String classPath, String methodPath) {
        classPath = classPath.replaceAll("^/+", "").replaceAll("/+$", "");
        methodPath = methodPath.replaceAll("^/+", "").replaceAll("/+$", "");
        return "/" + Stream.of(classPath, methodPath)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }
}