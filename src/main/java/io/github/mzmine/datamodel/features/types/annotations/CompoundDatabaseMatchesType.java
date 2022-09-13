/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CompoundAnnotationScoreType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundDatabaseMatchesType extends ListWithSubsType<CompoundDBAnnotation> implements
    AnnotationType {

  public static final List<DataType> subTypes = List.of(new CompoundDatabaseMatchesType(),
      new CompoundNameType(), new CompoundAnnotationScoreType(), new FormulaType(),
      new IonTypeType(), new SmilesStructureType(), new InChIStructureType(), new PrecursorMZType(),
      new MzPpmDifferenceType(), new NeutralMassType(), new RTType(), new CCSType(),
      new DatabaseMatchInfoType());
  private static final Logger logger = Logger.getLogger(
      CompoundDatabaseMatchesType.class.getName());
  private static final Map<Class<? extends DataType>, Function<CompoundDBAnnotation, Object>> mapper = Map.ofEntries(
      //
      createEntry(CompoundDatabaseMatchesType.class, CompoundDBAnnotation::getCompoundName), //
      createEntry(CompoundNameType.class, CompoundDBAnnotation::getCompoundName), //
      createEntry(CompoundAnnotationScoreType.class, CompoundDBAnnotation::getScore),
      createEntry(FormulaType.class, CompoundDBAnnotation::getFormula), //
      createEntry(IonTypeType.class, CompoundDBAnnotation::getAdductType), //
      createEntry(SmilesStructureType.class, CompoundDBAnnotation::getSmiles), //
      createEntry(InChIStructureType.class, match -> match.get(new InChIStructureType())), //
      createEntry(PrecursorMZType.class, CompoundDBAnnotation::getPrecursorMZ), //
      createEntry(MzPpmDifferenceType.class, match -> match.get(MzPpmDifferenceType.class)), //
      createEntry(NeutralMassType.class, match -> match.get(new NeutralMassType())), //
      createEntry(RTType.class, match -> match.get(new RTType())), //
      createEntry(CCSType.class, CompoundDBAnnotation::getCCS),
      createEntry(DatabaseMatchInfoType.class, match -> match.get(DatabaseMatchInfoType.class)),
      createEntry(CommentType.class, match -> match.get(CommentType.class)));

  public CompoundDatabaseMatchesType() {
  }

  private static <T, R> R mapOrElse(@Nullable final T input, @NotNull final Function<T, R> mapper,
      @Nullable R elze) {
    return input != null ? mapper.apply(input) : elze;
  }

  @Override
  public @NotNull String getUniqueID() {
    return "compound_db_identity";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Compound DB";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  protected Map<Class<? extends DataType>, Function<CompoundDBAnnotation, Object>> getMapper() {
    return mapper;
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
      if (!(o instanceof CompoundDBAnnotation id)) {
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

    final List<CompoundDBAnnotation> ids = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      // todo remove upper branch in next release
      if (reader.getLocalName().equals(CompoundDBAnnotation.XML_ELEMENT_OLD)) {
        final CompoundDBAnnotation id = SimpleCompoundDBAnnotation.loadFromXML(reader, project,
            flist, row);
        if (id != null) {
          ids.add(id);
        }
      } else if (reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)) {
        final FeatureAnnotation id = FeatureAnnotation.loadFromXML(reader, project, flist, row);
        if (id instanceof CompoundDBAnnotation cid) {
          ids.add(cid);
        } else {
          logger.warning(
              () -> "Unexpected annotation type in compound annotations: " + id.getClass()
                  .getName());
        }
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }
}
