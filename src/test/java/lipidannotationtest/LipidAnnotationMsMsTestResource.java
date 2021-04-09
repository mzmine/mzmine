package lipidannotationtest;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;

public class LipidAnnotationMsMsTestResource {

  private double[] mzFragments;
  private IonizationType ionizationType;
  private ILipidAnnotation testLipid;

  public LipidAnnotationMsMsTestResource(double[] mzFragments, IonizationType ionizationType,
      ILipidAnnotation testLipid) {
    this.mzFragments = mzFragments;
    this.ionizationType = ionizationType;
    this.testLipid = testLipid;
  }

  public double[] getMzFragments() {
    return mzFragments;
  }

  public void setMzFragments(double[] mzFragments) {
    this.mzFragments = mzFragments;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public void setIonizationType(IonizationType ionizationType) {
    this.ionizationType = ionizationType;
  }

  public ILipidAnnotation getTestLipid() {
    return testLipid;
  }

  public void setTestLipid(ILipidAnnotation testLipid) {
    this.testLipid = testLipid;
  }

}
