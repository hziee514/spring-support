package me.wrh.spring.swagger;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author wurenhai
 * @since 2018/3/9
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(SwaggerConfiguration.class)
public @interface EnableSwagger2Doc {
}
