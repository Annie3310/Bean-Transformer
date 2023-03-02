package annotation;

import java.lang.annotation.*;

/**
 * Link a field to another field
 *
 * @author Jinyi Wang
 * @date 2023/2/17 15:42
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertyTransformer {
    String[] DEFAULT_VALUE = {"5xtdB"};
    String[] value() default {"5xtdB"};
}
