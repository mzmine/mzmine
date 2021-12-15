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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ALogPType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.EnzymeType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.IonTypeType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ReactionType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

public class BioTransformerAnnotationImpl extends SimpleCompoundDBAnnotation implements
    BioTransformerAnnotation {

  public static final String XML_TYPE_NAME = "biotransformerannotation";

  protected BioTransformerAnnotationImpl() {
  }

  public BioTransformerAnnotationImpl(String formula, double calculatedMz, IonType ionType,
      String smiles, String inchi, String reaction, String enzyme, Float alogP) {
    put(new FormulaType(), formula);
    put(new PrecursorMZType(), calculatedMz);
    put(new IonTypeType(), ionType);
    put(new SmilesStructureType(), smiles);
    put(new InChIStructureType(), inchi);
    put(new ReactionType(), reaction);
    put(new EnzymeType(), enzyme);
    put(new ALogPType(), alogP);
  }

  /**
   * Writes this object to xml using the given writer. A new element for this object is created in
   * the given method.
   *
   * @param writer The writer.
   */
  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException {
    writer.writeStartElement(CompoundDBAnnotation.XML_ELEMENT);
    writer.writeAttribute(CompoundDBAnnotation.XML_TYPE_ATTRIBUTE, XML_TYPE_NAME);
    writer.writeAttribute(CompoundDBAnnotation.XML_NUM_ENTRIES_ATTR, String.valueOf(size()));

    for (Entry<DataType<?>, Object> entry : entrySet()) {
      final DataType<?> key = entry.getKey();
      final Object value = entry.getValue();

      writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
      writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, key.getUniqueID());
      try { // catch here, so we can easily debug and don't destroy the flist while saving in case
        // an unexpected exception happens
        key.saveToXML(writer, value, flist, row, null, null);
      } catch (XMLStreamException e) {
        final Object finalVal = value;
        logger.warning(
            () -> "Error while writing data type " + key.getClass().getSimpleName() + " with value "
                + finalVal + " to xml.");
        e.printStackTrace();
      }
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  public static CompoundDBAnnotation loadFromXML(XMLStreamReader reader, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT)
        && reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE).equals(XML_TYPE_NAME))) {
      throw new IllegalStateException("Invalid xml element to load BiotransformerAnnotaion from.");
    }

    final BioTransformerAnnotation id = new BioTransformerAnnotationImpl();
    final int numEntries = Integer.parseInt(reader.getAttributeValue(null, XML_NUM_ENTRIES_ATTR));

    int i = 0;
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();

      if(!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        continue;
      }

      final DataType<?> typeForId = DataTypes.getTypeForId(
          reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
      if (typeForId != null) {
        Object o = typeForId.loadFromXML(reader, flist, row, null, null);
        id.put((DataType) typeForId, o);
      }
      i++;

      if (i > numEntries) {
        break;
      }
    }

    if (i > numEntries) {
      throw new IllegalStateException(String.format(
          "Finished reading db annotation, but did not find all types. Expected %d, found %d.",
          numEntries, i));
    }
    return id;
  }
}
