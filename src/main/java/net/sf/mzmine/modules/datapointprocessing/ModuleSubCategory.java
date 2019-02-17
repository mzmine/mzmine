package net.sf.mzmine.modules.datapointprocessing;

public enum ModuleSubCategory {
    MASSDETECTION("Mass detection"), ISOTOPES("Isotopes"), IDENTIFICATION("Identification");
  
  private final String name;
  
  ModuleSubCategory(String name){
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
}
