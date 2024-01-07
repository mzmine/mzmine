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
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CosineScoreType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

  private static final Map<Class<? extends DataType>, Function<SpectralDBAnnotation, Object>> mapper = Map.ofEntries(
      createEntry(SpectralLibraryMatchesType.class, match -> match),
      createEntry(CompoundNameType.class,
          match -> match.getEntry().getField(DBEntryField.NAME).orElse("").toString()),
      createEntry(FormulaType.class,
          match -> match.getEntry().getField(DBEntryField.FORMULA).orElse("").toString()),
      createEntry(IonAdductType.class,
          match -> match.getEntry().getField(DBEntryField.ION_TYPE).orElse("").toString()),
      createEntry(SmilesStructureType.class,
          match -> match.getEntry().getField(DBEntryField.SMILES).orElse("").toString()),
      createEntry(InChIStructureType.class,
          match -> match.getEntry().getField(DBEntryField.INCHI).orElse("").toString()),
      createEntry(CosineScoreType.class, match -> (float) match.getSimilarity().getScore()),
      createEntry(MatchingSignalsType.class, match -> match.getSimilarity().getOverlap()),
      createEntry(PrecursorMZType.class,
          match -> match.getEntry().getField(DBEntryField.PRECURSOR_MZ).orElse(null)),
      createEntry(NeutralMassType.class,
          match -> match.getEntry().getField(DBEntryField.EXACT_MASS).orElse(null)),
      createEntry(CCSType.class, match -> match.getEntry().getOrElse(DBEntryField.CCS, null)),
      createEntry(CCSRelativeErrorType.class, SpectralDBAnnotation::getCCSError));
  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new SpectralLibraryMatchesType(),
      new CompoundNameType(), new IonAdductType(), new FormulaType(), new SmilesStructureType(),
      new InChIStructureType(), new PrecursorMZType(), new NeutralMassType(), new CosineScoreType(),
      new MatchingSignalsType(), new CCSType(), new CCSRelativeErrorType());

  @Override
  protected Map<Class<? extends DataType>, Function<SpectralDBAnnotation, Object>> getMapper() {
    return mapper;
  }

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
