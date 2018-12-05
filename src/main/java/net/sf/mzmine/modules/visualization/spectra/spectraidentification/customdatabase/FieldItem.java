package net.sf.mzmine.modules.visualization.spectra.spectraidentification.customdatabase;

public enum FieldItem {

  FIELD_MZ("m/z"), //
  FIELD_NAME("Identity"); //

  private final String name;

  FieldItem(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
