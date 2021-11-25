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
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
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
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidMatchListType extends ListWithSubsType<MatchedLipid> implements AnnotationType {

  private static final Map<Class<? extends DataType>, Function<MatchedLipid, Object>> mapper =
      Map.ofEntries(
          createEntry(LipidMatchListType.class, match -> match),
          createEntry(IonAdductType.class, match -> match.getIonizationType().getAdductName()),
          createEntry(FormulaType.class, match -> MolecularFormulaManipulator
              .getString(match.getLipidAnnotation().getMolecularFormula())),
          createEntry(CommentType.class,
              match -> match.getComment() != null ? match.getComment() : ""),
          createEntry(LipidAnnotationMsMsScoreType.class, l -> l.getMsMsScore().floatValue()),
          createEntry(LipidSpectrumType.class, match -> true),
          createEntry(MzPpmDifferenceType.class, match -> {
            // calc ppm error?
            double exactMass = getExactMass(match);
            return (float)((exactMass - match.getAccurateMz()) / exactMass) * 1000000;
          })
      );

  private static final List<DataType> subTypes = List.of(//
      new LipidMatchListType(), //
      new IonAdductType(), //
      new FormulaType(), //
      new CommentType(), //
      new MzPpmDifferenceType(), //
      new LipidAnnotationMsMsScoreType(), //
      new LipidSpectrumType());

  private static double getExactMass(MatchedLipid match) {
    return MolecularFormulaManipulator.getMass(match.getLipidAnnotation().getMolecularFormula(),
        AtomContainerManipulator.MonoIsotopic) + match.getIonizationType().getAddedMass();
  }

  @Override
  protected Map<Class<? extends DataType>, Function<MatchedLipid, Object>> getMapper() {
    return mapper;
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_annotations";
  }

  @Override
  public @NotNull
  List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public @NotNull
  String getHeaderString() {
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
      throw new IllegalArgumentException("Wrong value type for data type: "
                                         + this.getClass().getName() + " value class: " + value
                                             .getClass());
    }

    for (Object o : list) {
      if (!(o instanceof MatchedLipid id)) {
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

    List<MatchedLipid> ids = new ArrayList<>();

    while (reader.hasNext()
           && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(MatchedLipid.XML_ELEMENT)) {
        MatchedLipid id = MatchedLipid.loadFromXML(reader, flist.getRawDataFiles());
        ids.add(id);
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }
}
