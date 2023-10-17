package io.github.mzmine.modules.visualization.kendrickmassplot;

public enum KendrickPlotDataTypes {

  M_OVER_Z("m/z", "m/z"),//
  KENDRICK_MASS("Kendrick Mass", "KM"),//
  KENDRICK_MASS_DEFECT("Kendrick Mass Defect", "KMD"),//
  REMAINDER_OF_KENDRICK_MASS("Remainder of Kendrick Mass", "RKM"),//
  RETENTION_TIME("Retention Time", "rt"),//
  MOBILITY("Mobility", "mobility"),//
  INTENSITY("Intensity", "Int."),//
  AREA("Area", "Area"),//
  TAILING_FACTOR("Tailing Factor", "Tailing Factor"),//
  ASYMMETRY_FACTOR("Asymmetry Factor", "Asymmetry Factor"),//
  FWHM("FWHM", "FWHM");//

  private final String name;

  private final String abbr;

  KendrickPlotDataTypes(String name, String abbr) {
    this.name = name;
    this.abbr = abbr;
  }

  public String getName() {
    return name;
  }

  public String getAbbr() {
    return abbr;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public boolean isKendrickType() {
    return switch (this) {
      case KENDRICK_MASS, KENDRICK_MASS_DEFECT, REMAINDER_OF_KENDRICK_MASS -> true;
      default -> false;
    };
  }
}
