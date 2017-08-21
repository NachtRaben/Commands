package com.nachtraben.orangeslice.command;

public @interface CmdAttribute {

    /**
     * The identifier of the attribute.
     *
     * @return the string
     */
    String identifier();


    /**
     * The value of this attribute.
     *
     * @return the string
     */
    String value() default "";

}
