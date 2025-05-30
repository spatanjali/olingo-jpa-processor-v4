package com.sap.olingo.jpa.processor.cb.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

import com.sap.olingo.jpa.processor.cb.exceptions.NotImplementedException;
import com.sap.olingo.jpa.processor.cb.joiner.SqlConvertible;
import com.sap.olingo.jpa.processor.cb.joiner.StringBuilderCollector;

/**
 *
 * @author Oliver Grande
 *
 */
abstract class PredicateImpl extends ExpressionImpl<Boolean> implements Predicate {

  private static final int REQUIRED_NO_OPERATOR = 2;
  protected final List<SqlConvertible> expressions;

  static Predicate and(final Predicate[] restrictions) {
    if (restrictions == null || arrayIsEmpty(restrictions, REQUIRED_NO_OPERATOR))
      throw new IllegalArgumentException("Parameter 'restrictions' has to have at least 2 elements");
    var predicate = restrictions[0];
    for (int i = 1; i < restrictions.length; i++) {
      predicate = new AndPredicate(predicate, restrictions[i]);
    }
    return predicate;
  }

  static Predicate or(final Predicate[] restrictions) {
    if (restrictions == null || arrayIsEmpty(restrictions, REQUIRED_NO_OPERATOR))
      throw new IllegalArgumentException("Parameter 'restrictions' has to have at least 2 elements");
    var predicate = restrictions[0];
    for (int i = 1; i < restrictions.length; i++) {
      predicate = new OrPredicate(predicate, restrictions[i]);
    }
    return predicate;
  }

  private static boolean arrayIsEmpty(final Predicate[] restrictions, final int requiredNoElements) {
    for (int i = 0; i < restrictions.length; i++) {
      if (restrictions[i] == null)
        return true;
    }
    return restrictions.length < requiredNoElements;
  }

  protected PredicateImpl(final SqlConvertible... expressions) {
    super();
    this.expressions = Collections.unmodifiableList(Arrays.asList(expressions));
  }

  @Override
  public Selection<Boolean> alias(final String name) {
    alias = Optional.ofNullable(name);
    return this;
  }

  @Override
  public <X> Expression<X> as(final Class<X> type) {
    throw new NotImplementedException();
  }

  @Override
  @CheckForNull
  public String getAlias() {
    return alias.orElse(null);
  }

  @Override
  public List<Selection<?>> getCompoundSelectionItems() {
    throw new NotImplementedException();
  }

  @Override
  public List<Expression<Boolean>> getExpressions() {
    return asExpression();
  }

  @Override
  public Class<? extends Boolean> getJavaType() {
    throw new NotImplementedException();
  }

  /**
   * Whether the predicate has been created from another
   * predicate by applying the <code>Predicate.not()</code> method
   * or the <code>CriteriaBuilder.not()</code> method.
   * @return boolean indicating if the predicate is
   * a negated predicate
   */
  @Override
  public boolean isNegated() {
    return false;
  }

  @Override
  public Predicate not() {
    return new NotPredicate(this);
  }

  private List<Expression<Boolean>> asExpression() {
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return "PredicateImpl [sql=" + asSQL(new StringBuilder()) + "]";
  }

  static class AndPredicate extends BooleanPredicate {

    AndPredicate(final Expression<Boolean> x, final Expression<Boolean> y) {
      super(x, y);
    }

    /**
     * Return the boolean operator for the predicate.
     * If the predicate is simple, this is <code>AND</code>.
     * @return boolean operator for the predicate
     */
    @Override
    public BooleanOperator getOperator() {
      return Predicate.BooleanOperator.AND;
    }
  }

  static class BetweenExpressionPredicate extends PredicateImpl {

    private final ExpressionImpl<?> attribute;

    BetweenExpressionPredicate(final ExpressionImpl<?> attribute, final Expression<?> left, final Expression<?> right) {
      super((SqlConvertible) left, (SqlConvertible) right);
      this.attribute = attribute;
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      statement.append(OPENING_BRACKET);
      this.attribute.asSQL(statement)
          .append(" ")
          .append(SqlKeyWords.BETWEEN)
          .append(" ");
      this.expressions.get(0).asSQL(statement)
          .append(" ")
          .append(SqlKeyWords.AND)
          .append(" ");
      return this.expressions.get(1).asSQL(statement).append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }
  }

  static class BinaryExpressionPredicate extends PredicateImpl {
    private final Operation expression;

    BinaryExpressionPredicate(final Operation operation, final Expression<?> left, final Expression<?> right) {
      super((SqlConvertible) left, (SqlConvertible) right);
      this.expression = operation;
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      statement.append(OPENING_BRACKET);
      this.expressions.get(0).asSQL(statement)
          .append(" ")
          .append(expression)
          .append(" ");
      return this.expressions.get(1).asSQL(statement).append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }

    enum Operation {
      EQ("="), NE("<>"), GT(">"), GE(">="), LT("<"), LE("<=");

      private final String keyWord;

      private Operation(final String keyWord) {
        this.keyWord = keyWord;
      }

      @Override
      public String toString() {
        return keyWord;
      }
    }
  }

  abstract static class BooleanPredicate extends PredicateImpl {

    BooleanPredicate(final Expression<Boolean> x, final Expression<Boolean> y) {
      super((SqlConvertible) x, (SqlConvertible) y);
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      statement.append(OPENING_BRACKET);
      expressions.get(0).asSQL(statement)
          .append(" ")
          .append(getOperator())
          .append(" ");
      expressions.get(1).asSQL(statement);
      statement.append(CLOSING_BRACKET);
      return statement;
    }

    @Override
    public String toString() {
      return "AndPredicate [left=" + expressions.get(0) + ", right=" + expressions.get(1) + "]";
    }
  }

  static class LikePredicate extends PredicateImpl {
    private final ParameterExpression<String, ?> pattern;
    private final Optional<ParameterExpression<Character, ?>> escape;

    public LikePredicate(final Expression<String> column, final ParameterExpression<String, ?> pattern) {
      this(column, pattern, Optional.empty());
    }

    public LikePredicate(final Expression<String> column, final ParameterExpression<String, ?> pattern,
        final Optional<ParameterExpression<Character, ?>> escapeChar) {
      super((SqlConvertible) column);
      this.pattern = pattern;
      this.escape = escapeChar;
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      statement.append(OPENING_BRACKET);
      this.expressions.get(0).asSQL(statement)
          .append(" ")
          .append(SqlKeyWords.LIKE)
          .append(" ");
      this.pattern.asSQL(statement);
      this.escape.ifPresent(esc -> statement
          .append(" ")
          .append(SqlKeyWords.ESCAPE)
          .append(" "));
      this.escape.ifPresent(esc -> esc.asSQL(statement));
      return statement.append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }
  }

  static class NotPredicate extends PredicateImpl {

    private final SqlConvertible positive;

    NotPredicate(final SqlConvertible predicate) {
      this.positive = predicate;
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      statement
          .append(OPENING_BRACKET)
          .append(SqlKeyWords.NOT)
          .append(" ");
      return positive.asSQL(statement)
          .append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }

    @Override
    public boolean isNegated() {
      return true;
    }
  }

  static class NullPredicate extends PredicateImpl {

    private final SqlNullCheck check;

    NullPredicate(@Nonnull final Expression<?> expression, @Nonnull final SqlNullCheck check) {
      super((SqlConvertible) Objects.requireNonNull(expression));
      this.check = Objects.requireNonNull(check);
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {
      return expressions.get(0).asSQL(statement.append(OPENING_BRACKET))
          .append(" ").append(check).append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }
  }

  static class OrPredicate extends BooleanPredicate {

    OrPredicate(final Expression<Boolean> x, final Expression<Boolean> y) {
      super(x, y);
    }

    /**
     * Return the boolean operator for the predicate.
     * If the predicate is simple, this is <code>AND</code>.
     * @return boolean operator for the predicate
     */
    @Override
    public BooleanOperator getOperator() {
      return Predicate.BooleanOperator.OR;
    }
  }

  static class In<X> extends PredicateImpl implements CriteriaBuilder.In<X> {
    private Optional<Expression<? extends X>> expression;
    private Optional<List<ParameterExpression<X, X>>> values;
    private ParameterBuffer parameter;

    @SuppressWarnings("unchecked")
    In(final List<Path<?>> paths, final Subquery<?> subquery) {
      this(paths.stream().map(path -> (Path<Comparable<?>>) path).toList(), null);
      this.expression = Optional.of((Expression<? extends X>) Objects.requireNonNull(subquery));
    }

    In(final List<Path<Comparable<?>>> paths, final ParameterBuffer parameter) {
      super(new CompoundPathImpl(paths));
      this.values = Optional.empty();
      this.expression = Optional.empty();
      this.parameter = parameter;
    }

    @SuppressWarnings("unchecked")
    In(final Path<?> path, final ParameterBuffer parameter) {
      this(Collections.singletonList((Path<Comparable<?>>) path), parameter);
    }

    @Override
    public StringBuilder asSQL(final StringBuilder statement) {

      return expression.map(exp -> asSubQuerySQL(exp, statement))
          .orElseGet(() -> values.map(list -> asFixValueSQL(list, statement))
              .orElseThrow());
    }

    private StringBuilder asFixValueSQL(final List<ParameterExpression<X, X>> list, final StringBuilder statement) {
      prepareInExpression(statement);
      statement.append(list.stream()
          .collect(new StringBuilderCollector.ExpressionCollector(statement, ", ")));
      return statement.append(CLOSING_BRACKET);
    }

    private StringBuilder asSubQuerySQL(final Expression<? extends X> exp, final StringBuilder statement) {
      prepareInExpression(statement);
      return ((SqlConvertible) Objects.requireNonNull(exp)).asSQL(statement).append(CLOSING_BRACKET);
    }

    private void prepareInExpression(final StringBuilder statement) {
      expressions.get(0).asSQL(statement)
          .append(" ")
          .append(SqlKeyWords.IN)
          .append(" ")
          .append(OPENING_BRACKET);
    }

    @Override
    public jakarta.persistence.criteria.CriteriaBuilder.In<X> value(final X value) {
      if (this.expression.isPresent())
        throw new IllegalStateException("Do not add a fixed value if an expression is already present");
      values.ifPresentOrElse(list -> list.add(parameter.addValue(value)), () -> createValues(value));
      return this;
    }

    private void createValues(final X value) {
      // parameter.addValue(value)
      values = Optional.of(new ArrayList<>());
      values.get().add(parameter.addValue(value));
    }

    @Override
    public jakarta.persistence.criteria.CriteriaBuilder.In<X> value(final Expression<? extends X> value) {
      if (this.values.isPresent())
        throw new IllegalStateException("Do not add an expression if a fixed value is already present");
      if (this.expression.isPresent())
        throw new NotImplementedException();
      this.expression = Optional.of(Objects.requireNonNull(value));
      return this;
    }

    @Override
    public BooleanOperator getOperator() {
      return BooleanOperator.AND;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Expression<X> getExpression() {
      final CompoundPath paths = ((CompoundPath) expressions.get(0));
      return paths.isEmpty() ? null : (Expression<X>) paths.getFirst();
    }

  }

  static class SubQuery extends PredicateImpl {
    private final SqlConvertible query;
    private final SqlSubQuery operator;

    public SubQuery(@Nonnull final Subquery<?> subquery, @Nonnull final SqlSubQuery operator) {
      this.query = (SqlConvertible) Objects.requireNonNull(subquery);
      this.operator = operator;
    }

    @Override
    public StringBuilder asSQL(@Nonnull final StringBuilder statement) {
      statement.append(operator)
          .append(" ")
          .append(OPENING_BRACKET);
      return query.asSQL(statement).append(CLOSING_BRACKET);
    }

    @Override
    public BooleanOperator getOperator() {
      return null;
    }

  }
}
