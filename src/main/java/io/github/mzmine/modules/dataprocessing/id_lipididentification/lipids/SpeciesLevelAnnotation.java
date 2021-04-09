package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import org.openscience.cdk.interfaces.IMolecularFormula;

public class SpeciesLevelAnnotation implements ILipidAnnotation {

  private ILipidClass lipidClass;
  private String annotation;
  private static final LipidAnnotationLevel lipidAnnotationLevel =
      LipidAnnotationLevel.SPECIES_LEVEL;
  private IMolecularFormula molecularFormula;
  private int numberOfCarbons;
  private int numberOfDBEs;

  public SpeciesLevelAnnotation(ILipidClass lipidClass, String annotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs) {
    this.lipidClass = lipidClass;
    this.annotation = annotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
  }

  @Override
  public ILipidClass getLipidClass() {
    return lipidClass;
  }

  @Override
  public void setLipidClass(ILipidClass lipidClass) {
    this.lipidClass = lipidClass;
  }

  @Override
  public String getAnnotation() {
    return annotation;
  }

  @Override
  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  @Override
  public LipidAnnotationLevel getLipidAnnotationLevel() {
    return lipidAnnotationLevel;
  }

  @Override
  public IMolecularFormula getMolecularFormula() {
    return molecularFormula;
  }

  public int getNumberOfCarbons() {
    return numberOfCarbons;
  }

  public void setNumberOfCarbons(int numberOfCarbons) {
    this.numberOfCarbons = numberOfCarbons;
  }

  public int getNumberOfDBEs() {
    return numberOfDBEs;
  }

  public void setNumberOfDBEs(int numberOfDBEs) {
    this.numberOfDBEs = numberOfDBEs;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
    result = prime * result + numberOfCarbons;
    result = prime * result + numberOfDBEs;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SpeciesLevelAnnotation other = (SpeciesLevelAnnotation) obj;
    if (annotation == null) {
      if (other.annotation != null)
        return false;
    } else if (!annotation.equals(other.annotation))
      return false;
    if (numberOfCarbons != other.numberOfCarbons)
      return false;
    if (numberOfDBEs != other.numberOfDBEs)
      return false;
    return true;
  }

}
