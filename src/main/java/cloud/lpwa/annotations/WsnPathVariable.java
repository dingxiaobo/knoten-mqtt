package cloud.lpwa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通配内容会被赋值 如果结尾是/将会被删除
 * <p>
 * 请使用  String | Integer | Boolean | Long | Short | Double | Float
 * 当 multi == true 时  只能用String接收
 * <p>
 * <p>
 * Created by hzdxb on 2017/2/25.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface WsnPathVariable {


    /**
     * 第n个占位符 从0 开始计数
     */
    int value() default 0;

    /*
    false是适配 + ,true则适配# 只能用于String
     */
    boolean multi() default false;


}
