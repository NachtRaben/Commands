package com.nachtraben.orangeslice.command;

/**
 * This can be used to add attribute modifiers to {@Link Cmd} annotations.
 */
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
