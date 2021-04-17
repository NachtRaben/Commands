package dev.armadeus.command.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CmdAttribute {

    String name();


    /**
     * The value of this attribute.
     *
     * @return the string
     */
    String value() default "";

}
