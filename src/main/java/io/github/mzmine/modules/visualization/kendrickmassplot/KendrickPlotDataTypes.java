package io.github.mzmine.modules.visualization.kendrickmassplot;

public enum KendrickPlotDataTypes {

  M_OVER_Z("m/z"),//
  KENDRICK_MASS("Kendrick Mass"),//
  KENDRICK_MASS_DEFECT("Kendrick Mass Defect"),//
  RETENTION_TIME("Retention Time"),//
  MOBILITY("Mobility"),//
  INTENSITY("Intensity"),//
  AREA("Area"),//
  TAILING_FACTOR("Tailing Factor"),//
  ASYMMETRY_FACTOR("Asymmetry Factor"),//
  FWHM("FWHM");//

  private final String name;

  KendrickPlotDataTypes(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public boolean isKendrickType() {
    return switch (this) {
      case KENDRICK_MASS, KENDRICK_MASS_DEFECT -> true;
      default -> false;
    };
  }
}
