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

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.JsonStringType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireParentType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.identifiers.CASType;
import io.github.mzmine.datamodel.features.types.identifiers.EntryIdType;
import io.github.mzmine.datamodel.features.types.identifiers.InternalIdType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.ExplainedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SimilarityType;
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
      new SimilarityType(),//
      new MatchingSignalsType(),//
      new ExplainedIntensityPercentType(),//
      new IonAdductType(), //
      new FormulaType(),//
      new MolecularStructureType(),//
      new SmilesStructureType(),//
      new InChIStructureType(),//
      // classifiers
      new ClassyFireSuperclassType(), new ClassyFireClassType(), new ClassyFireSubclassType(),
      new ClassyFireParentType(), new NPClassifierSuperclassType(), new NPClassifierClassType(),
      new NPClassifierPathwayType(), //
      new NeutralMassType(),//
      new PrecursorMZType(),//
      new MzAbsoluteDifferenceType(),//
      new MzPpmDifferenceType(),//
      new RtAbsoluteDifferenceType(),//
      new CCSType(),//
      new CCSRelativeErrorType(),//
      new CommentType(), //
      new EntryIdType(), //
      new CASType(),  //
      new InternalIdType(), //
      new JsonStringType()
      );

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
  public <K> @Nullable K map(@NotNull final DataType<K> subType, final SpectralDBAnnotation match) {
    final SpectralLibraryEntry entry = match.getEntry();
    return (K) switch (subType) {
      case SpectralLibraryMatchesType _ -> match;
      case CompoundNameType _ -> entry.getField(DBEntryField.NAME).orElse("").toString();
      case FormulaType _ -> entry.getField(DBEntryField.FORMULA).orElse("").toString();
      case IonAdductType _ -> entry.getField(DBEntryField.ION_TYPE).orElse("").toString();
      case MolecularStructureType _ -> entry.getStructure();
      case SmilesStructureType _ -> entry.getField(DBEntryField.SMILES).orElse("").toString();
      case InChIStructureType _ -> entry.getField(DBEntryField.INCHI).orElse("").toString();
      case ClassyFireSuperclassType _ ->
          entry.getField(DBEntryField.CLASSYFIRE_SUPERCLASS).orElse("").toString();
      case ClassyFireClassType _ ->
          entry.getField(DBEntryField.CLASSYFIRE_CLASS).orElse("").toString();
      case ClassyFireSubclassType _ ->
          entry.getField(DBEntryField.CLASSYFIRE_SUBCLASS).orElse("").toString();
      case ClassyFireParentType _ ->
          entry.getField(DBEntryField.CLASSYFIRE_PARENT).orElse("").toString();
      case NPClassifierSuperclassType _ ->
          entry.getField(DBEntryField.NPCLASSIFIER_SUPERCLASS).orElse("").toString();
      case NPClassifierClassType _ ->
          entry.getField(DBEntryField.NPCLASSIFIER_CLASS).orElse("").toString();
      case NPClassifierPathwayType _ ->
          entry.getField(DBEntryField.NPCLASSIFIER_PATHWAY).orElse("").toString();
      case SimilarityType _ -> (float) match.getSimilarity().getScore();
      case ExplainedIntensityPercentType __ -> match.getSimilarity().getExplainedLibraryIntensity();
      case MatchingSignalsType _ -> match.getSimilarity().getOverlap();
      case PrecursorMZType _ -> entry.getField(DBEntryField.PRECURSOR_MZ).orElse(null);
      case NeutralMassType _ -> entry.getField(DBEntryField.EXACT_MASS).orElse(null);
      case CCSType _ -> entry.getOrElse(DBEntryField.CCS, null);
      case CCSRelativeErrorType _ -> match.getCCSError();
      case RtAbsoluteDifferenceType _ -> match.getRtAbsoluteError();
      case MzAbsoluteDifferenceType _ -> match.getMzAbsoluteError();
      case MzPpmDifferenceType _ -> match.getMzPpmError();
      case CommentType _ -> entry.getOrElse(DBEntryField.COMMENT, null);
      case EntryIdType _ -> entry.getOrElse(DBEntryField.ENTRY_ID, null);
      case CASType _ -> entry.getOrElse(DBEntryField.CAS, null);
      case InternalIdType _ -> entry.getOrElse(DBEntryField.INTERNAL_ID, null);
      case JsonStringType _ -> entry.getOrElse(DBEntryField.JSON_STRING, null);
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


  @Override
  public int getPrefColumnWidth() {
    return 150;
  }
}
