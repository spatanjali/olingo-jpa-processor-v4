package com.sap.olingo.jpa.metadata.odata.v4.core.terms;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OData core annotation <i>Computed</i>: <br>
 * A value for this property is generated on both insert and update.<p>
 *
 * AppliesTo: Property
 * @author Oliver Grande
 *
 */
@Target(FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Computed {
  boolean value() default true;
}
