/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.CompoundDBIdentity;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundDatabaseMatchesType extends ListWithSubsType<CompoundDBIdentity> {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_database_identity";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Compound DB";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(new CompoundDatabaseMatchesType(), new FormulaType(), new IonAdductType(),
        new SmilesStructureType(), new InChIStructureType(), new PrecursorMZType(),
        new NeutralMassType(), new RTType(), new CCSType());
  }

  @Override
  protected Map<Class<? extends DataType>, Function<CompoundDBIdentity, Object>> getMapper() {
    return Map.ofEntries( //
        createEntry(CompoundDatabaseMatchesType.class, SimpleFeatureIdentity::getName), //
        createEntry(FormulaType.class,
            match -> match.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA)), //
        createEntry(IonAdductType.class,
            match -> match.getPropertyValue(FeatureIdentity.PROPERTY_ADDUCT)), //
        createEntry(SmilesStructureType.class,
            match -> match.getPropertyValue(FeatureIdentity.PROPERTY_SMILES)), //
        createEntry(InChIStructureType.class,
            match -> match.getPropertyValue(FeatureIdentity.PROPERTY_INCHI_KEY)), //
        createEntry(PrecursorMZType.class,
            match -> mapOrElse(match.getPropertyValue(FeatureIdentity.PROPERTY_PRECURSORMZ),
                Double::parseDouble, null)), //
        createEntry(RTType.class,
            match -> mapOrElse(match.getPropertyValue(FeatureIdentity.PROPERTY_RT),
                Float::parseFloat, null)), //
        createEntry(CCSType.class,
            match -> mapOrElse(match.getPropertyValue(FeatureIdentity.PROPERTY_CCS),
                Float::parseFloat, null)) //
    );
  }

  private static <T, R> R mapOrElse(@Nullable final T input, @NotNull final Function<T, R> mapper,
      @Nullable R elze) {
    return input != null ? mapper.apply(input) : elze;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(value == null) {
      return;
    }

    if(!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value
              .getClass());
    }

    for (Object o : list) {
      if (!(o instanceof CompoundDBIdentity id)) {
        continue;
      }

      id.saveToXML(writer);
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    List<FeatureIdentity> ids = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT) && reader
          .getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
          .equals(CompoundDBIdentity.XML_IDENTITY_TYPE)) {
        FeatureIdentity id = FeatureIdentity.loadFromXML(reader, flist.getRawDataFiles());
        ids.add(id);
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }
}
