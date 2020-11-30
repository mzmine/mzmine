package io.github.mzmine.gui.preferences;

public enum UnitFormat {

  ROUND_BRACKED("Label (unit)"), SQUARE_BRACKET("Label [unit]"), DIVIDE("Label / unit");

  private final String representativeString;

  UnitFormat(String representativeString) {
    this.representativeString = representativeString;
  }



  public String format(String label, String unit) {
    switch(this) {
      case SQUARE_BRACKET:
        return label + " [" + unit + "]";
      case ROUND_BRACKED:
        return label + " (" + unit + ")";
      case DIVIDE:
        return  label + " / " + unit;
      default:
        return  label + " / " + unit;
    }
  }

  @Override
  public String toString() {
    return representativeString;
  }
}
