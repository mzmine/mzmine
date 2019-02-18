package net.sf.mzmine.modules.datapointprocessing.datamodel;

public interface DPPResult<DATATYPE> {

  public String getName();

  public DATATYPE getValue();

//  public String toString();
  
  public String generateLabel();

  public Classification getClassification();

  public static enum Classification {
    STRING("String"), ISOTOPE_PATTERN("Isotope pattern"), FRAGMENTATION("Fragmentation"), ADDUCT(
        "Addukt");

    Classification(String name) {
      this.name = name;
    }

    private final String name;

    public String getName() {
      return name;
    }
  }

}
