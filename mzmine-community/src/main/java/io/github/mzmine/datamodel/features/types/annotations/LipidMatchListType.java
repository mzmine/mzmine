/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.ExplainedIntensityPercentType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidMatchListType extends ListWithSubsType<MatchedLipid> implements AnnotationType {

  private static final List<DataType> subTypes = List.of(//
      new LipidMatchListType(), //
      new IonAdductType(), //
      new FormulaType(), //
      new CommentType(), //
      new MzPpmDifferenceType(), //
      new ExplainedIntensityPercentType(),//
      new LipidSpectrumType());

  private static double getExactMass(MatchedLipid match) {
    return MolecularFormulaManipulator.getMass(match.getLipidAnnotation().getMolecularFormula(),
        AtomContainerManipulator.MonoIsotopic) + match.getIonizationType().getAddedMass();
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_annotations";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  protected <K> @Nullable K map(@NotNull final DataType<K> subType, final MatchedLipid match) {
    return (K) switch (subType) {
      case LipidMatchListType __ -> match;
      case IonAdductType __ -> match.getIonizationType().getAdductName();
      case FormulaType __ ->
          MolecularFormulaManipulator.getString(match.getLipidAnnotation().getMolecularFormula());
      case CommentType __ -> match.getComment() != null ? match.getComment() : "";
      case ExplainedIntensityPercentType __ -> match.getMsMsScore().floatValue();
      case LipidSpectrumType __ -> true;
      case MzPpmDifferenceType __ -> {
        // calc ppm error?
        double exactMass = getExactMass(match);
        yield (float) ((exactMass - match.getAccurateMz()) / exactMass) * 1000000;
      }
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map".formatted(subType.toString()));
    };
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Lipid Annotation";
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
    }

    for (Object o : list) {
      if (!(o instanceof MatchedLipid id)) {
        continue;
      }

      id.saveToXML(writer);
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
          && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    List<MatchedLipid> ids = new ArrayList<>();
    final List<RawDataFile> currentRawDataFiles = project.getCurrentRawDataFiles();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(MatchedLipid.XML_ELEMENT)) {
        MatchedLipid id = MatchedLipid.loadFromXML(reader, currentRawDataFiles);
        ids.add(id);
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
