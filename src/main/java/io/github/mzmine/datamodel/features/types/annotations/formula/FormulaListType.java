/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.annotations.formula;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
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

/**
 * A collection of annotation types related to a list of molecular formulas stored in the {@link
 * SimpleFormulaListType}. Includes scores, neutral mass, etc.
 */
public class FormulaListType extends ListWithSubsType<ResultFormula> implements AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new FormulaListType(),
      new FormulaMassType(), new RdbeType(), new MzPpmDifferenceType(),
      new MzAbsoluteDifferenceType(), new IsotopePatternScoreType(), new MsMsScoreType(),
      new CombinedScoreType());

  private static final Map<Class<? extends DataType>, Function<ResultFormula, Object>> mapper = Map.ofEntries(
      createEntry(FormulaListType.class, formula -> formula),
      createEntry(FormulaMassType.class, formula -> formula.getExactMass()),
      createEntry(RdbeType.class, formula -> formula.getRDBE()),
      createEntry(MzPpmDifferenceType.class, formula -> formula.getPpmDiff()),
      createEntry(MzAbsoluteDifferenceType.class, formula -> formula.getAbsoluteMzDiff()),
      createEntry(IsotopePatternScoreType.class, formula -> formula.getIsotopeScore()),
      createEntry(MsMsScoreType.class, formula -> formula.getMSMSScore()),
      createEntry(CombinedScoreType.class, formula -> formula.getScore(10, 3, 1)));

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  protected Map<Class<? extends DataType>, Function<ResultFormula, Object>> getMapper() {
    return mapper;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Formulas";
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "formulas";
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
          "Value for type " + this.getClass().getName() + " is not a List. " + value.getClass()
              .toString());
    }

    for (Object o : list) {
      if (!(o instanceof ResultFormula formula)) {
        continue;
      }

      formula.saveToXML(writer);
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

    List<ResultFormula> formulas = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if(reader.getLocalName().equals(ResultFormula.XML_ELEMENT)) {
        final ResultFormula formula = ResultFormula.loadFromXML(reader);
        formulas.add(formula);
      }
    }

    return formulas.isEmpty() ? null : formulas;
  }
}
