package cloud.lpwa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解只能使用在byte[]的方法参数中，Message的payload值将会被赋值到该方法参数
 * Created by hzdxb on 2017/2/25.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface WsnBytes {
}
