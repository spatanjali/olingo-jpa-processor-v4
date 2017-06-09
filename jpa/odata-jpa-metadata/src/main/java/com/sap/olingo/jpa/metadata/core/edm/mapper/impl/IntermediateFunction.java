package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction.ReturnType;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunctionParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAFunctionParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAFunctionResultParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Mapper, that is able to convert different metadata resources into a edm function metadata. It is important to know
 * that:
 * <cite>Functions MUST NOT have observable side effects and MUST return a single instance or a collection of instances
 * of any type.</cite>
 * <p>For details about Function metadata see:
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398010"
 * >OData Version 4.0 Part 3 - 12.2 Element edm:Function</a>
 * @author Oliver Grande
 *
 */

abstract class IntermediateFunction extends IntermediateModelElement implements JPAFunction {
  protected CsdlFunction edmFunction;
  protected final EdmFunction jpaFunction;
  protected final IntermediateSchema schema;

  IntermediateFunction(final JPAEdmNameBuilder nameBuilder, final EdmFunction jpaFunction,
      final IntermediateSchema schema, final String internalName) throws ODataJPAModelException {

    super(nameBuilder, internalName);
    this.jpaFunction = jpaFunction;
    this.schema = schema;
  }

  @Override
  public JPAFunctionResultParameter getResultParameter() {
    return new IntermediatResultFunctionParameter(jpaFunction.returnType());
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmFunction == null) {
      edmFunction = new CsdlFunction();
      edmFunction.setName(getExternalName());
      edmFunction.setParameters(returnNullIfEmpty(determineEdmInputParameter()));
      edmFunction.setReturnType(determineEdmResultType(jpaFunction.returnType()));
      edmFunction.setBound(jpaFunction.isBound());
      // TODO edmFunction.setComposable(isComposable)
      edmFunction.setComposable(false);
      // TODO edmFunction.setEntitySetPath(entitySetPath) for bound functions

    }
  }

  @Override
  CsdlFunction getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmFunction;
  }

  String getUserDefinedFunction() {
    return jpaFunction.functionName();
  }

  boolean hasFunctionImport() {
    return jpaFunction.hasFunctionImport();
  }

  boolean isBound() throws ODataJPAModelException {
    return getEdmItem().isBound();
  }

  protected abstract List<CsdlParameter> determineEdmInputParameter() throws ODataJPAModelException;

  protected abstract CsdlReturnType determineEdmResultType(final ReturnType returnType) throws ODataJPAModelException;

  protected class IntermediatFunctionParameter implements JPAFunctionParameter {
    private final EdmFunctionParameter jpaParameter;
    private final String internalName;
    private final String externalName;
    private final Class<?> type;

    IntermediatFunctionParameter(final EdmFunctionParameter jpaParameter) {
      this.jpaParameter = jpaParameter;
      this.internalName = jpaParameter.parameterName();
      this.externalName = jpaParameter.name();
      this.type = jpaParameter.type();
    }

    public IntermediatFunctionParameter(EdmFunctionParameter jpaParameter, String externalName,
        String internalName, Class<?> type) {
      this.jpaParameter = jpaParameter;
      this.internalName = internalName;
      this.externalName = externalName;
      this.type = type;
    }

    @Override
    public String getInternalName() {
      return internalName;
    }

    @Override
    public String getName() {
      return externalName;
    }

    @Override
    public Class<?> getType() {
      return type;
    }

    @Override
    public Integer getMaxLength() {
      return jpaParameter.maxLength();
    }

    @Override
    public Integer getPrecision() {
      return jpaParameter.precision();
    }

    @Override
    public Integer getScale() {
      return jpaParameter.scale();
    }

    @Override
    public FullQualifiedName getTypeFQN() throws ODataJPAModelException {
      return JPATypeConvertor.convertToEdmSimpleType(jpaParameter.type()).getFullQualifiedName();
    }
  }

  private class IntermediatResultFunctionParameter implements JPAFunctionResultParameter {
    private final ReturnType jpaReturnType;

    public IntermediatResultFunctionParameter(final ReturnType jpaReturnType) {
      this.jpaReturnType = jpaReturnType;
    }

    @Override
    public Class<?> getType() {
      return jpaReturnType.type();
    }

    @Override
    public Integer getMaxLength() {
      return jpaReturnType.maxLength();
    }

    @Override
    public Integer getPrecision() {
      return jpaReturnType.precision();
    }

    @Override
    public Integer getScale() {
      return jpaReturnType.scale();
    }

    @Override
    public FullQualifiedName getTypeFQN() {
      return edmFunction.getReturnType().getTypeFQN();
    }

    @Override
    public boolean isCollection() {
      return jpaReturnType.isCollection();
    }

  }
}
