package com.sky.annotation;

import com.sky.enumPer.OperationPerm;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /**
     * 操作权限
     * @return
     */
    OperationPerm value();

}
