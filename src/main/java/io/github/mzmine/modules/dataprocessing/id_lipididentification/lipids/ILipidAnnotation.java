package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import org.openscience.cdk.interfaces.IMolecularFormula;

public interface ILipidAnnotation {

  ILipidClass getLipidClass();

  void setLipidClass(ILipidClass lipidClasss);

  String getAnnotation();

  void setAnnotation(String annotation);

  LipidAnnotationLevel getLipidAnnotationLevel();

  IMolecularFormula getMolecularFormula();
}
