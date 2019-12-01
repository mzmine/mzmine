package io.github.mzmine.datamodel.fx.test;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Data {

  private StringProperty name;

  Data(String i) {
    name = new SimpleStringProperty(i);
  }

  public String getMyName() {
    return name.get();
  }

  public void setMyName(String name) {
    this.name.set(name);
  }

  public StringProperty myNameProperty() {
    return name;
  }
}
