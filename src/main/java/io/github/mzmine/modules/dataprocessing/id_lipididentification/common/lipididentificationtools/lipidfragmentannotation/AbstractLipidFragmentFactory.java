package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public abstract class AbstractLipidFragmentFactory {

  protected Range<Double> mzTolRangeMSMS;
  protected ILipidAnnotation lipidAnnotation;
  protected IonizationType ionizationType;
  protected LipidFragmentationRule[] rules;
  protected DataPoint dataPoint;
  protected Scan msMsScan;

  public AbstractLipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan) {
    this.mzTolRangeMSMS = mzTolRangeMSMS;
    this.lipidAnnotation = lipidAnnotation;
    this.ionizationType = ionizationType;
    this.rules = rules;
    this.dataPoint = dataPoint;
    this.msMsScan = msMsScan;
  }

  public List<LipidFragment> findCommonLipidFragment() {
    List<LipidFragment> lipidFragment = new ArrayList<>();
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForCommonRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragment.add(detectedFragment);
        break;
      }
    }
    return lipidFragment;
  }

  private LipidFragment checkForCommonRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    return switch (ruleType) {
      case HEADGROUP_FRAGMENT ->
          checkForHeadgroupFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case HEADGROUP_FRAGMENT_NL ->
          checkForHeadgroupFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      default -> null;
    };
  }

  private LipidFragment checkForHeadgroupFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(fragmentFormula);
    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzFragmentExact, fragmentFormula, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula formulaNL = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    IMolecularFormula lipidFormula = null;
    try {
      lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    rule.getIonizationType().ionizeFormula(lipidFormula);
    IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidFormula, formulaNL);
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(fragmentFormula);

    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzFragmentExact,
          MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  protected IMolecularFormula ionizeFragmentBasedOnPolarity(IMolecularFormula formula,
      PolarityType polarityType) {
    if (polarityType.equals(PolarityType.NEGATIVE)) {
      IonizationType.NEGATIVE.ionizeFormula(formula);
      return formula;
    } else if (polarityType.equals(PolarityType.POSITIVE)) {
      IonizationType.POSITIVE.ionizeFormula(formula);
      return formula;
    }
    return formula;
  }

}
