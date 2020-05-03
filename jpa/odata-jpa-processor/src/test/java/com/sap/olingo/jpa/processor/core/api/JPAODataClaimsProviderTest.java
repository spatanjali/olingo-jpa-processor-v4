package com.sap.olingo.jpa.processor.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JPAODataClaimsProviderTest {

  private JPAODataClaimsProvider cut;

  @BeforeEach
  public void setup() {
    cut = new JPAODataClaimsProvider();
  }

  @Test
  public void checkAddSinglePairReturnsOne() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    final List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(1, claims.size());
  }

  @Test
  public void checkAddThreeSinglePairsReturnsThree() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    cut.add("Test", new JPAClaimsPair<>("Willi"));
    cut.add("Test", new JPAClaimsPair<>("Walter"));
    final List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(3, claims.size());
  }

  @Test
  public void checkNotProvidedAttributeReturnsEmptyList() {
    final List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(0, claims.size());
  }

  @Test
  public void checkAddTwoAttributesSinglePairs() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    cut.add("Dummy", new JPAClaimsPair<>("Willi"));

    List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(1, claims.size());

    claims = cut.get("Dummy");
    assertNotNull(claims);
    assertEquals(1, claims.size());
  }

  @Test
  public void checkCreateWithUser() {
    assertNotNull(new JPAODataClaimsProvider("Willi"));
  }

  @Test
  public void checkUserReturnedWhenInConstructor() {
    assertTrue(new JPAODataClaimsProvider("Willi").user().isPresent());
  }

  @Test
  public void checkUserNotPresentWhenNotInConstructor() {
    assertFalse(cut.user().isPresent());
  }

}
