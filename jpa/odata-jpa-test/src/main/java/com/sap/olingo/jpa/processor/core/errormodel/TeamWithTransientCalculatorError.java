package com.sap.olingo.jpa.processor.core.errormodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmTransient;

@Entity(name = "TeamWithTransientError")
@Table(schema = "\"OLINGO\"", name = "\"Team\"")
public class TeamWithTransientCalculatorError {

  @Id
  @Column(name = "\"TeamKey\"")
  private String iD;

  @Column(name = "\"Name\"")
  private String name;

  @EdmTransient(requiredAttributes = { "name" }, calculator = TransientPropertyCalculatorTwoConstructors.class)
  private String completeName;
}
