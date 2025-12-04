package io.mzmine.reports;

public class FeatureSummary {

  private final String id;
  private final String mz;
  private final String rtInMinutes;
  private final String ccs;
  private final String height;
  private final String area;
  private final String compoundName;
  private final String identifier;
  private final String areaPercent;

  public FeatureSummary(String id, String mz, String rtInMinutes, String ccs, String height,
      String area, String compoundName, String identifier, String areaPercent) {
    this.id = id;
    this.mz = mz;
    this.rtInMinutes = rtInMinutes;
    this.ccs = ccs;
    this.height = height;
    this.area = area;
    this.compoundName = compoundName;
    this.identifier = identifier;
    this.areaPercent = areaPercent;
  }

  public String getId() {
    return id;
  }

  public String getMz() {
    return mz;
  }

  public String getRtInMinutes() {
    return rtInMinutes;
  }

  public String getCcs() {
    return ccs;
  }

  public String getHeight() {
    return height;
  }

  public String getArea() {
    return area;
  }

  public String getCompoundName() {
    return compoundName;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getAreaPercent() {
    return areaPercent;
  }
}
