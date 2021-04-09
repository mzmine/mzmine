package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;

public class LipidAnnotationSummaryType extends ListDataType<MatchedLipid>
    implements AnnotationType, EditableColumnType {

  @Override
  public String getHeaderString() {
    return "Lipid Name";
  }

}
