package net.sf.mzmine.modules.datapointprocessing.datamodel;

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
