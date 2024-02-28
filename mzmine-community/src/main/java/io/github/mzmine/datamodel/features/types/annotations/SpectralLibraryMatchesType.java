/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CosineScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.ExplainedIntensityPercentType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spectral library matches in a list
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SpectralLibraryMatchesType extends ListWithSubsType<SpectralDBAnnotation> implements
    AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of( //
      new SpectralLibraryMatchesType(), //
      new CompoundNameType(), //
      new CosineScoreType(),//
      new MatchingSignalsType(),//
      new ExplainedIntensityPercentType(),//
      new IonAdductType(), //
      new FormulaType(),//
      new SmilesStructureType(),//
      new InChIStructureType(),//
      new NeutralMassType(),//
      new PrecursorMZType(),//
      new MzAbsoluteDifferenceType(),//
      new MzPpmDifferenceType(),//
      new RtAbsoluteDifferenceType(),//
      new CCSType(),//
      new CCSRelativeErrorType(),//
      new CommentType());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "spectral_db_matches";
  }

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  protected <K> @Nullable K map(@NotNull final DataType<K> subType,
      final SpectralDBAnnotation match) {
    final SpectralLibraryEntry entry = match.getEntry();
    return (K) switch (subType) {
      case SpectralLibraryMatchesType __ -> match;
      case CompoundNameType __ -> entry.getField(DBEntryField.NAME).orElse("").toString();
      case FormulaType __ -> entry.getField(DBEntryField.FORMULA).orElse("").toString();
      case IonAdductType __ -> entry.getField(DBEntryField.ION_TYPE).orElse("").toString();
      case SmilesStructureType __ -> entry.getField(DBEntryField.SMILES).orElse("").toString();
      case InChIStructureType __ -> entry.getField(DBEntryField.INCHI).orElse("").toString();
      case CosineScoreType __ -> (float) match.getSimilarity().getScore();
      case ExplainedIntensityPercentType __ -> match.getSimilarity().getExplainedLibraryIntensity();
      case MatchingSignalsType __ -> match.getSimilarity().getOverlap();
      case PrecursorMZType __ -> entry.getField(DBEntryField.PRECURSOR_MZ).orElse(null);
      case NeutralMassType __ -> entry.getField(DBEntryField.EXACT_MASS).orElse(null);
      case CCSType __ -> entry.getOrElse(DBEntryField.CCS, null);
      case CCSRelativeErrorType __ -> match.getCCSError();
      case RtAbsoluteDifferenceType __ -> match.getRtAbsoluteError();
      case MzAbsoluteDifferenceType __ -> match.getMzAbsoluteError();
      case MzPpmDifferenceType __ -> match.getMzPpmError();
      case CommentType __ -> entry.getOrElse(DBEntryField.COMMENT, null);
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map".formatted(subType.toString()));
    };
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Spectral match";
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
      if (!(o instanceof SpectralDBAnnotation id)) {
        continue;
      }

      id.saveToXML(writer, flist, row);
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

    List<FeatureAnnotation> ids = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      // todo remove first branch in a few versions so we can delete SpectralDBFeatureIdentity
      if (reader.getLocalName().equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT)
          && reader.getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
          .equals(SpectralDBFeatureIdentity.XML_IDENTITY_TYPE)) {
        FeatureIdentity id = FeatureIdentity.loadFromXML(reader, project,
            project.getCurrentRawDataFiles());
        ids.add(new SpectralDBAnnotation((SpectralDBFeatureIdentity) id));
      } else if (reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)
          && reader.getAttributeValue(null, FeatureAnnotation.XML_TYPE_ATTR)
          .equals(SpectralDBAnnotation.XML_ATTR)) {
        ids.add(
            SpectralDBAnnotation.loadFromXML(reader, project, project.getCurrentRawDataFiles()));
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
