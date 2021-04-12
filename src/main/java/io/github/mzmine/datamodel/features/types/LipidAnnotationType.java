package io.github.mzmine.datamodel.features.types;

import java.util.List;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class LipidAnnotationType extends ModularType implements AnnotationType {

  private final List<DataType> subTypes = List.of(//
      new LipidAnnotationSummaryType(), //
      new IonAdductType(), //
      new FormulaType(), //
      new CommentType(), //
      new LipidMsOneErrorType(), //
      new LipidAnnotationMsMsScoreType(), //
      new LipidSpectrumType());

  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public String getHeaderString() {
    return "Lipid Annotation";
  }

  @Override
  public ModularTypeProperty createProperty() {
    ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on
    // this object
    property.get(LipidAnnotationSummaryType.class)
        .addListener((ListChangeListener<MatchedLipid>) change -> {
          ObservableList<? extends MatchedLipid> summaryProperty = change.getList();
          boolean firstElementChanged = false;
          while (change.next()) {
            firstElementChanged = firstElementChanged || change.getFrom() == 0;
          }
          if (firstElementChanged) {
            // first list elements has changed - set all other fields
            setCurrentElement(property, summaryProperty.isEmpty() ? null : summaryProperty.get(0),
                summaryProperty.size());
          }
        });

    return property;
  }

  /**
   * On change of the first list element, change all the other sub types.
   * 
   * @param data
   * @param match
   */
  private void setCurrentElement(ModularTypeProperty data, MatchedLipid match,
      int numberOfAnnotations) {
    if (match == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof LipidAnnotationSummaryType)) {
          data.set(type, null);
        }
      }
    } else {

      // update selected values
      data.set(FormulaType.class,
          MolecularFormulaManipulator.getString(match.getLipidAnnotation().getMolecularFormula()));
      data.set(IonAdductType.class, match.getIonizationType().getAdductName());
      if (match.getComment() != null) {
        data.set(CommentType.class, match.getComment());
      }
      // Calc rel mass deviation;
      double exactMass =
          MolecularFormulaManipulator.getMass(match.getLipidAnnotation().getMolecularFormula(),
              AtomContainerManipulator.MonoIsotopic) + match.getIonizationType().getAddedMass();
      double relMassDev = ((exactMass - match.getAccurateMz()) / exactMass) * 1000000;
      data.set(LipidMsOneErrorType.class, relMassDev);
      data.set(LipidAnnotationMsMsScoreType.class, match.getMsMsScore());
      data.set(LipidSpectrumType.class, null);// ??????
    }
  }
}
