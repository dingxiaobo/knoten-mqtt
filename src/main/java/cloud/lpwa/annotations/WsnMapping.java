package cloud.lpwa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * topic映射
 * Created by hzdxb on 2017/2/25.
 */


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WsnMapping {

    /**
     * topic映射
     */
    String[] value() default {""};

    /**
     * qos 如果 qos配置了一个值 那么所有mapping都遵循这个qos，如果qos配置了多个值，那么qos与mapping一一对应
     */
    int[] qos() default {-1};

    /**
     * 重复信息过滤<br/>
     * <p>默认同一条消息被多次接收时只运行该方法一遍</p>
     */
    boolean duplicatable() default false;

}
