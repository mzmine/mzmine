package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;

public class LipidFragmentationRule {

	private PolarityType polarityType;
	private IonizationType ionizationType;
	private LipidFragmentationRuleType lipidFragmentationRuleType;
	private LipidAnnotationLevel lipidFragmentInformationLevelType;
	private String molecularFormula;

	public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
			LipidFragmentationRuleType lipidFragmentationRuleType,
			LipidAnnotationLevel lipidFragmentInformationLevelType) {
		this.polarityType = polarityType;
		this.ionizationType = ionizationType;
		this.lipidFragmentationRuleType = lipidFragmentationRuleType;
		this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
		this.molecularFormula = "";
	}

	public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
			LipidFragmentationRuleType lipidFragmentationRuleType,
			LipidAnnotationLevel lipidFragmentInformationLevelType, String molecularFormula) {
		this.polarityType = polarityType;
		this.ionizationType = ionizationType;
		this.lipidFragmentationRuleType = lipidFragmentationRuleType;
		this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
		this.molecularFormula = molecularFormula;
	}

	public PolarityType getPolarityType() {
		return polarityType;
	}

	public IonizationType getIonizationType() {
		return ionizationType;
	}

	public LipidFragmentationRuleType getLipidFragmentationRuleType() {
		return lipidFragmentationRuleType;
	}

	public LipidAnnotationLevel getLipidFragmentInformationLevelType() {
		return lipidFragmentInformationLevelType;
	}

	public String getMolecularFormula() {
		return molecularFormula;
	}

	@Override
	public String toString() {
		return ionizationType + ", " + lipidFragmentationRuleType + " " + molecularFormula;
	}
}
