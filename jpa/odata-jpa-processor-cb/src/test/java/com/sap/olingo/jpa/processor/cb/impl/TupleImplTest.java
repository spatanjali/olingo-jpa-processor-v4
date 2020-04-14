package com.sap.olingo.jpa.processor.cb.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.processor.cb.api.ProcessorSelection;

public class TupleImplTest {
  private static final String SECOND_VALUE = "Second";
  private static final String THIRD_VALUE = "Third";
  private static final String FIRST_VALUE = "First";
  private Tuple cut;
  private Object[] values = { "Hello", "World", 3 };
  private Map<String, Integer> selectionIndex;
  private List<Entry<String, JPAAttribute>> selPath;

  @BeforeEach
  public void setup() {

    selPath = new ArrayList<>(3);
    selectionIndex = new HashMap<>(3);
    selectionIndex.put(FIRST_VALUE, 0);
    selectionIndex.put(SECOND_VALUE, 1);
    selectionIndex.put(THIRD_VALUE, 2);

    selPath.add(new ProcessorSelection.SelectionAttribute(FIRST_VALUE, mockAttribute(FIRST_VALUE, String.class)));
    selPath.add(new ProcessorSelection.SelectionAttribute(SECOND_VALUE, mockAttribute(SECOND_VALUE, String.class)));
    selPath.add(new ProcessorSelection.SelectionAttribute(THIRD_VALUE, mockAttribute(THIRD_VALUE, Integer.class)));
    cut = new TupleImpl(values, selPath, selectionIndex);
  }

  private JPAAttribute mockAttribute(final String alias, final Class<?> clazz) {
    final JPAAttribute a = mock(JPAAttribute.class);
    when(a.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return clazz;
      }
    });
    return a;
  }

  @Test
  public void testToArrayReturnsCopyOf() {
    assertArrayEquals(values, cut.toArray());
    assertNotEquals(values, cut.toArray());
  }

  @Test
  public void testGetByIndexReturnsCorrectValue() {
    assertEquals("World", cut.get(1));
    assertEquals("Hello", cut.get(0));
  }

  @Test
  public void testGetByIndexThrowsExceptionOnInvalidIndex() {
    assertThrows(IllegalArgumentException.class, () -> cut.get(5));
    assertThrows(IllegalArgumentException.class, () -> cut.get(-1));
  }

  @Test
  public void testGetByAliasReturnsCorrectValue() {
    assertEquals("World", cut.get(SECOND_VALUE));
    assertEquals("Hello", cut.get(FIRST_VALUE));
  }

  @Test
  public void testGetByAliseThrowsExceptionOnInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> cut.get("Willi"));
  }

  @Test
  public void testGetByIndexWithCastReturnsCorrectValue() {
    assertEquals(3, cut.get(2, Number.class));
  }

  @Test
  public void testGetByIndexWithCastThrowsExceptionOnInvalidIndex() {
    assertThrows(IllegalArgumentException.class, () -> cut.get(5, Number.class));
    assertThrows(IllegalArgumentException.class, () -> cut.get(-1, Number.class));
  }

  @Test
  public void testGetByIndexWithCastThrowsExceptionOnInvalidCast() {
    assertThrows(IllegalArgumentException.class, () -> cut.get(2, Double.class));
  }

  @Test
  public void testGetByAliasWithCastReturnsCorrectValue() {
    assertEquals(3, cut.get(THIRD_VALUE, Number.class));
  }

  @Test
  public void testGetByAliseWithCastThrowsExceptionOnInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> cut.get("Willi", Number.class));
  }

  @Test
  public void testGetByAliasWithCastThrowsExceptionOnInvalidCast() {
    assertThrows(IllegalArgumentException.class, () -> cut.get(THIRD_VALUE, Double.class));
  }

  @Test
  public void testGetTupleElements() {
    final List<TupleElement<?>> act = cut.getElements();
    boolean secondFound = false;
    assertEquals(3, act.size());
    for (final TupleElement<?> t : act) {
      if (SECOND_VALUE.equals(t.getAlias())) {
        assertEquals(String.class, t.getJavaType());
        assertEquals("World", cut.get(t));
        secondFound = true;
      }
    }
    assertTrue(secondFound);
  }
}
