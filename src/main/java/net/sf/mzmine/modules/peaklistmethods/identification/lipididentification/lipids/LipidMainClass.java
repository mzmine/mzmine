package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidCoreClass.CoreClasses;

public class LipidMainClass extends AbstractLipidClass {

  public enum MainClasses {
    MONORADYLGLYCEROLS(CoreClasses.GLYCEROLIPIDS), //
    DIRADYLGLYCEROLS(CoreClasses.GLYCEROLIPIDS), //
    TRIRADYLGLYCEROLS(CoreClasses.GLYCEROLIPIDS); //
    // GLYCEROPHOSPHOETHANOLAMINES, //
    // GLYCEROPHOSPHOSERINES, //
    // GLYCEROPHOSPHOGLYCEROLS, //
    // GLYCEROPHOSPHOGLYCEROPHOSPHATES, //
    // GLYCEROPHOSPHOINOSITOLS, //
    // GLYCEROPHOSPHATES, //
    // CARDIOLIPIN, //
    // CDPGLYCEROLS;//

    // var
    private final CoreClasses coreClass;

    // counstruct
    MainClasses(CoreClasses coreClass) {
      this.coreClass = coreClass;
    }

    public CoreClasses getCoreClass() {
      return coreClass;
    }

  }

  // var
  private LipidCoreClass coreClass;

  LipidMainClass(String name, String abbr) {
    super(name, abbr);
  }

  public LipidCoreClass getCoreClass() {
    return coreClass;
  }

  public void setCoreClass(LipidCoreClass coreClass) {
    this.coreClass = coreClass;
  }

}
