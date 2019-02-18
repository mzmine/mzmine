package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

public abstract class DPPResult<T> {

  final String name;
  final T value;
  
  public DPPResult (String key, T value) {
    this.name = key;
    this.value = value;
  }
  
  public String getName() {
    return name;
  }

  public T getValue() {
    return value;
  }

//  public String toString();
  
  public abstract String generateLabel();

  public abstract Classification getClassification();

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
