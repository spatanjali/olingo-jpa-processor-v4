package com.sap.olingo.jpa.processor.cb.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPACollectionAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.cb.api.SqlConvertable;
import com.sap.olingo.jpa.processor.cb.api.SqlJoinType;
import com.sap.olingo.jpa.processor.cb.exeptions.NotImplementedException;

class CollectionJoinImpl<Z, X> extends AbstractJoinImp<Z, X> {

  private final JPACollectionAttribute attribute;

  CollectionJoinImpl(@Nonnull final JPAPath path, @Nonnull final FromImpl<?, Z> parent,
      @Nonnull final AliasBuilder aliasBuilder, @Nonnull CriteriaBuilder cb) throws ODataJPAModelException {

    super(determineEt(path, parent), parent, determinePath(path), aliasBuilder, cb);
    this.attribute = (JPACollectionAttribute) path.getLeaf();

    createOn(attribute.asAssociation().getJoinTable().getRawJoinInformation());
  }

  private static JPAPath determinePath(final JPAPath path) throws ODataJPAModelException {
    return ((JPACollectionAttribute) path.getLeaf()).asAssociation().getJoinTable().getEntityType() == null
        ? path : null;
  }

  private static JPAEntityType determineEt(final JPAPath path, final FromImpl<?, ?> parent)
      throws ODataJPAModelException {
    return Optional.ofNullable(((JPACollectionAttribute) path.getLeaf()).asAssociation().getJoinTable().getEntityType())
        .orElse(parent.st);
  }

  @Override
  public StringBuilder asSQL(final StringBuilder statment) {

    try {
      statment.append(" ")
          .append(SqlJoinType.byJoinType(getJoinType()))
          .append(" ")
          .append(attribute.asAssociation()
              .getJoinTable()
              .getTableName());

      tableAlias.ifPresent(p -> statment.append(" ").append(p));
      statment.append(" ON ");
      return ((SqlConvertable) on).asSQL(statment);
    } catch (ODataJPAModelException e) {
      throw new IllegalStateException("Target DB table of collection attribute &1 of &2"
          .replace("&1", attribute.getInternalName())
          .replace("&2", st.getInternalName())
          ,e);
    }
  }

  @Override
  List<Path<Object>> resolvePathElements() {
    final List<Path<Object>> pathList = new ArrayList<>();
    try {
      if (!attribute.isComplex()) {
        final JPAStructuredType source = attribute.asAssociation().getSourceType();
        pathList.add(new PathImpl<>(source.getPath(attribute.asAssociation().getAlias()),
            parent, st, tableAlias));
      } else {
        final JPAStructuredType source = attribute.getStructuredType();
        for (final JPAPath p : source.getPathList()) {
          pathList.add(new PathImpl<>(p, parent, st, tableAlias));
        }
      }
    } catch (ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
    return pathList;
  }

  @Override
  List<JPAPath> getPathList() {
    final List<JPAPath> pathList = new ArrayList<>();
    try {
      if (!attribute.isComplex()) {
        final JPAStructuredType source = attribute.asAssociation().getSourceType();
        final JPAPath path = source.getPath(this.alias.orElse(attribute.getExternalName()));
        pathList.add(path);
      } else {
        pathList.addAll(attribute.getStructuredType().getPathList().stream().filter(p -> !p.ignore()).collect(Collectors
            .toList()));
      }
    } catch (ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
    return pathList;
  }

  /**
   * Return the metamodel attribute corresponding to the join.
   * @return metamodel attribute corresponding to the join
   */
  @Override
  public Attribute<? super Z, ?> getAttribute() {
    throw new NotImplementedException();
  }

  @Override
  public JoinType getJoinType() {
    return JoinType.INNER;
  }
}
