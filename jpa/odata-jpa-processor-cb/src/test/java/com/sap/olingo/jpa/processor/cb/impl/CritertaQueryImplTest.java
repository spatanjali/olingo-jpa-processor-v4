package com.sap.olingo.jpa.processor.cb.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;

public class CritertaQueryImplTest extends BuilderBaseTest {
  private CriteriaQueryImpl<Object> cut;
  private CriteriaBuilder cb;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    cb = new CriteriaBuilderImpl(sd, new ParameterBuffer());
    cut = new CriteriaQueryImpl<>(Object.class, sd, cb);
  }

  @Test
  public void testSetDistinctTrue() {
    cut.distinct(true);
    assertTrue(cut.isDistinct());
  }

  @Test
  public void testSetDistinctFalse() {
    cut.distinct(false);
    assertFalse(cut.isDistinct());
  }

  @Test
  public void testCreateSelectFromAttribute() {
    final Root<?> adminDiv = cut.from(AdministrativeDivision.class);
    final CriteriaQuery<Object> act = cut.multiselect(adminDiv.get("codeID"));
    assertNotNull(act);
  }

  @Test
  public void testCreateOrderByWithArray() {
    final Root<?> adminDiv = cut.from(AdministrativeDivision.class);
    final CriteriaQuery<Object> act = cut.orderBy(cb.desc(adminDiv.get("codeID")));
    assertNotNull(act.getOrderList());
    assertEquals(1, act.getOrderList().size());
  }

  @Test
  public void testCreateOrderByWithList() {
    final Root<?> adminDiv = cut.from(AdministrativeDivision.class);
    final CriteriaQuery<Object> act = cut.orderBy(Arrays.asList(cb.desc(adminDiv.get("codeID")), cb.asc(adminDiv.get(
        "divisionCode"))));
    assertNotNull(act.getOrderList());
    assertEquals(2, act.getOrderList().size());
  }

  @Test
  public void testResetOrderByWithArray() {
    final Root<?> adminDiv = cut.from(AdministrativeDivision.class);
    final Order[] nullArray = null;
    CriteriaQuery<Object> act = cut.orderBy(Arrays.asList(cb.desc(adminDiv.get("codeID")), cb.asc(adminDiv.get(
        "divisionCode"))));
    act = cut.orderBy(nullArray);
    assertNotNull(act.getOrderList());
    assertEquals(0, act.getOrderList().size());
  }

  @Test
  public void testResetOrderByWithList() {
    final Root<?> adminDiv = cut.from(AdministrativeDivision.class);
    final List<Order> nullList = null;
    CriteriaQuery<Object> act = cut.orderBy(Arrays.asList(cb.desc(adminDiv.get("codeID")), cb.asc(adminDiv.get(
        "divisionCode"))));
    act = cut.orderBy(nullList);
    assertNotNull(act.getOrderList());
    assertEquals(0, act.getOrderList().size());
  }

  @Test
  public void testWithBaseClass() {
    final StringBuilder stmt = new StringBuilder();
    final Root<?> act = cut.from(Organization.class);
    cut.multiselect(act.get("iD"));
    assertEquals("SELECT E0.\"ID\" FROM \"OLINGO\".\"BusinessPartner\" E0 WHERE (E0.\"Type\" = ?1)",
        cut.asSQL(stmt).toString());
  }

  @Test
  public void testGroupBy() {
    final StringBuilder stmt = new StringBuilder();
    final Root<?> act = cut.from(Organization.class);
    cut.groupBy(act.get("aBCClass"), act.get("name2"));
    cut.multiselect(act.get("aBCClass"), act.get("name2"));
    assertEquals(
        "SELECT E0.\"ABCClass\", E0.\"NameLine2\" FROM \"OLINGO\".\"BusinessPartner\" E0 "
            + "WHERE (E0.\"Type\" = ?1) GROUP BY E0.\"ABCClass\", E0.\"NameLine2\"",
        cut.asSQL(stmt).toString());
  }

  @Test
  public void testReplaceGroupBydempty() {
    final Expression<?>[] nullArray = null;
    final StringBuilder stmt = new StringBuilder();
    final Root<?> act = cut.from(Organization.class);
    cut.groupBy(act.get("aBCClass"), act.get("name2"));
    cut.multiselect(act.get("aBCClass"), act.get("name2"));
    cut.groupBy(nullArray);
    assertEquals(
        "SELECT E0.\"ABCClass\", E0.\"NameLine2\" FROM \"OLINGO\".\"BusinessPartner\" E0 "
            + "WHERE (E0.\"Type\" = ?1)",
        cut.asSQL(stmt).toString());
  }

  @Test
  public void testHaving() {
    final StringBuilder stmt = new StringBuilder();
    final Root<?> act = cut.from(Organization.class);
    cut.having(cb.gt(cb.count(act.get("iD")), 1));
    cut.groupBy(act.get("name2"));
    cut.multiselect(act.get("aBCClass"), act.get("name2"));
    assertEquals(
        "SELECT E0.\"ABCClass\", E0.\"NameLine2\" FROM \"OLINGO\".\"BusinessPartner\" E0 "
            + "WHERE (E0.\"Type\" = ?2) "
            + "GROUP BY E0.\"NameLine2\" "
            + "HAVING (COUNT(E0.\"ID\") > ?1)",
        cut.asSQL(stmt).toString());
  }
}
