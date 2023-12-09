package io.github.mzmine.modules.visualization.vankrevelendiagram;

public enum VanKrevelenDiagramDataTypes {
  M_OVER_Z("m/z", "m/z"),//
  RETENTION_TIME("Retention Time", "rt"),//
  MOBILITY("Mobility", "mobility"),//
  INTENSITY("Intensity", "Int."),//
  AREA("Area", "Area"),//
  TAILING_FACTOR("Tailing Factor", "Tailing Factor"),//
  ASYMMETRY_FACTOR("Asymmetry Factor", "Asymmetry Factor"),//
  FWHM("FWHM", "FWHM");//

  private final String name;

  private final String abbr;

  VanKrevelenDiagramDataTypes(String name, String abbr) {
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

}
