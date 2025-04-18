package com.sap.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import jakarta.persistence.Tuple;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAODataPageExpandInfo;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAODataSkipTokenProvider;
import com.sap.olingo.jpa.processor.core.converter.JPACollectionResult;
import com.sap.olingo.jpa.processor.core.converter.JPAExpandResult;
import com.sap.olingo.jpa.processor.core.converter.JPAResultConverter;
import com.sap.olingo.jpa.processor.core.converter.JPATupleChildConverter;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAQueryException;

/**
 * Builds a hierarchy of expand results. One instance contains on the one hand of the result itself, a map which has the
 * join columns values of the parent as its key and on the other hand a map that point the results of the next expand.
 * The join columns are concatenated in the order they are stored in the corresponding Association Path.
 * @author Oliver Grande
 *
 */
public final class JPAExpandQueryResult implements JPAExpandResult, JPAConvertibleResult {
  private static final Map<String, List<Tuple>> EMPTY_RESULT;
  private final Map<JPAAssociationPath, JPAExpandResult> childrenResult;
  private final Map<String, List<Tuple>> jpaResult;
  private Map<String, EntityCollection> odataResult;
  private final Map<String, Long> counts;
  private final JPAEntityType jpaEntityType;
  private final Collection<JPAPath> requestedSelection;
  private final Optional<JPAODataSkipTokenProvider> skipTokenProvider;

  static {
    EMPTY_RESULT = new HashMap<>(1);
    putEmptyResult();
  }

  /**
   * Add an empty list as result for root to the EMPTY_RESULT. This is needed, as the conversion eats up the database
   * result.
   * @see JPATupleChildConverter
   * @return
   */
  private static Map<String, List<Tuple>> putEmptyResult() {
    EMPTY_RESULT.put(ROOT_RESULT_KEY, Collections.emptyList());
    return EMPTY_RESULT;
  }

  public JPAExpandQueryResult(final JPAEntityType jpaEntityType, final Collection<JPAPath> selectionPath) {
    this(putEmptyResult(), Collections.emptyMap(), jpaEntityType, selectionPath, Optional.empty());
  }

  public JPAExpandQueryResult(final Map<String, List<Tuple>> result, final Map<String, Long> counts,
      @Nonnull final JPAEntityType jpaEntityType, final Collection<JPAPath> selectionPath,
      final Optional<JPAODataSkipTokenProvider> skipTokenProvider) {

    Objects.requireNonNull(jpaEntityType);
    childrenResult = new HashMap<>();
    this.jpaResult = result;
    this.counts = counts;
    this.jpaEntityType = jpaEntityType;
    this.requestedSelection = selectionPath;
    this.skipTokenProvider = skipTokenProvider;
    this.odataResult = new HashMap<>(jpaResult.size());
  }

  @Override
  public Map<String, EntityCollection> asEntityCollection(final JPAResultConverter converter)
      throws ODataApplicationException {

    convert(converter, ROOT_RESULT_KEY, new ArrayList<>());
    return odataResult;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void convert(final JPAResultConverter converter) throws ODataApplicationException {
    if (odataResult == null) {
      for (final var childResult : childrenResult.values()) {
        childResult.convert(converter);
      }
      odataResult = (Map<String, EntityCollection>) converter.getResult(this, requestedSelection);
    }
  }

  EntityCollection convert(final JPAResultConverter converter, final String parentKey,
      final List<JPAODataPageExpandInfo> expandInfo)
      throws ODataApplicationException {

    if (!odataResult.containsKey(parentKey)) {
      // Convert collection properties up-front no re-implementation needed
      convertCollectionProperties(converter);
      odataResult.put(parentKey, converter.getResult(this, requestedSelection, parentKey, expandInfo));
    }
    return odataResult.get(parentKey);

  }

  @Override
  public JPAExpandResult getChild(final JPAAssociationPath associationPath) {
    return childrenResult.get(associationPath);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.org.jpa.processor.core.converter.JPAExpandResult#getChildren()
   */
  @Override
  public Map<JPAAssociationPath, JPAExpandResult> getChildren() {
    return childrenResult;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.org.jpa.processor.core.converter.JPAExpandResult#getCount()
   */
  @Override
  public Long getCount(final String key) {
    return counts != null ? counts.get(key) : null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.org.jpa.processor.core.converter.JPAExpandResult#getEntityType()
   */
  @Override
  public JPAEntityType getEntityType() {
    return jpaEntityType;
  }

  public long getNoResults() {
    return jpaResult.size();
  }

  public long getNoResultsDeep() {
    long count = 0;
    for (final Entry<String, List<Tuple>> result : jpaResult.entrySet()) {
      count += result.getValue().size();
    }
    return count;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.org.jpa.processor.core.converter.JPAExpandResult#getResult(java.lang.String)
   */
  @Override
  public List<Tuple> getResult(final String key) {
    return jpaResult.get(key);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.org.jpa.processor.core.converter.JPAExpandResult#hasCount()
   */
  @Override
  public boolean hasCount() {
    return counts != null;
  }

  @Override
  public void putChildren(final Map<JPAAssociationPath, JPAExpandResult> childResults)
      throws ODataApplicationException {

    for (final JPAAssociationPath child : childResults.keySet()) {
      if (childrenResult.get(child) != null)
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_EXPAND_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    childrenResult.putAll(childResults);
  }

  @Override
  public Map<String, List<Tuple>> getResults() {
    return jpaResult;
  }

  @Override
  public EntityCollection getEntityCollection(final String key, final JPAResultConverter converter,
      final List<JPAODataPageExpandInfo> expandInfo) throws ODataApplicationException {

    return jpaResult.containsKey(key)
        ? convert(converter, key, expandInfo)
        : new EntityCollection();
  }

  @Override
  public Optional<JPAKeyBoundary> getKeyBoundary(final JPAODataRequestContextAccess requestContext,
      final List<JPANavigationPropertyInfo> hops) throws ODataJPAProcessException {
    try {
      if (!jpaResult.get(ROOT_RESULT_KEY).isEmpty()
          && (requestContext.getUriInfo().getExpandOption() != null
              || collectionPropertyRequested(requestContext))
          && (requestContext.getUriInfo().getTopOption() != null
              || requestContext.getUriInfo().getSkipOption() != null)) {

        final JPAKeyPair boundary = new JPAKeyPair(jpaEntityType.getKey(), requestContext.getQueryDirectives());
        for (final Tuple tuple : jpaResult.get(ROOT_RESULT_KEY)) {
          @SuppressWarnings("rawtypes")
          final Map<JPAAttribute, Comparable> key = createKey(tuple);
          boundary.setValue(key);
        }
        return Optional.of(new JPAKeyBoundary(hops.size(), boundary));
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    return JPAConvertibleResult.super.getKeyBoundary(requestContext, hops);
  }

  private boolean collectionPropertyRequested(final JPAODataRequestContextAccess requestContext)
      throws ODataJPAModelException {
    if (!jpaEntityType.getCollectionAttributesPath().isEmpty()) {
      final SelectOption selectOptions = requestContext.getUriInfo().getSelectOption();
      if (SelectOptionUtil.selectAll(selectOptions)) {
        return true;
      } else {
        for (final SelectItem item : selectOptions.getSelectItems()) {
          final String pathItem = item.getResourcePath().getUriResourceParts().stream().map(path -> (path
              .getSegmentValue())).collect(Collectors.joining(JPAPath.PATH_SEPARATOR));
          if (this.jpaEntityType.getCollectionAttribute(pathItem) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @SuppressWarnings("rawtypes")
  private Map<JPAAttribute, Comparable> createKey(final Tuple tuple) throws ODataJPAModelException {
    final Map<JPAAttribute, Comparable> keyMap = new HashMap<>(jpaEntityType.getKey().size());
    for (final JPAAttribute key : jpaEntityType.getKey()) {
      keyMap.put(key, (Comparable) tuple.get(key.getExternalName()));
    }
    return keyMap;
  }

  @Override
  public String getSkipToken(final List<JPAODataPageExpandInfo> foreignKeyStack) {
    return skipTokenProvider
        .map(provider -> provider.get(foreignKeyStack))
        .map(this::convertToken)
        .orElse(null);
  }

  private String convertToken(final Object skipToken) {
    if (skipToken instanceof final String s)
      return s;
    return skipToken.toString();
  }

  private void convertCollectionProperties(final JPAResultConverter converter) throws ODataApplicationException {
    for (final var childResult : childrenResult.values()) {
      if (childResult instanceof JPACollectionResult)
        childResult.convert(converter);
    }
  }

}
