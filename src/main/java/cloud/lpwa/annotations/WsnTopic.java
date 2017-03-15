package cloud.lpwa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解只能使用在String的方法参数中，参数奖被赋值为topic
 * Created by hzdxb on 2017/2/25.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface WsnTopic {
}
