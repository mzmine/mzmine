package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public abstract class AbstractLipidClass {

  private final String name;
  private final String abbr;

  public AbstractLipidClass(String name, String abbr) {
    super();
    this.name = name;
    this.abbr = abbr;
  }

  public String getAbbr() {
    return abbr;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }
}
