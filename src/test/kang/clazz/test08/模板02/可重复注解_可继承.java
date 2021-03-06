package test.kang.clazz.test08.模板02;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 可重复注解必须关联一个容器
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(可重复注解的容器_可继承.class)
public @interface 可重复注解_可继承 {
    String str();
}
