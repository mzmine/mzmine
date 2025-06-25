/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.MolecularStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesIsomericStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.datamodel.structures.StructureParser;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Basic class for a compound annotation. The idea is not for it to be observable or so, but to
 * carry a flexible amount of data while providing a set of minimum defined entries.
 */
public class SimpleCompoundDBAnnotation implements CompoundDBAnnotation {

  // TODO remove all references to this in next release
  public static final String XML_TYPE_NAME_OLD = "simplecompounddbannotation";

  public static final String XML_ATTR = "simple_compound_db_annotation";

  private static final Logger logger = Logger.getLogger(SimpleCompoundDBAnnotation.class.getName());
  /**
   * sort map by order in {@link DBEntryField} then natural order of data types
   */
  protected final Map<DataType, Object> data = new TreeMap<>(
      Comparator.comparing(DBEntryField::fromDataType).thenComparing(DataType::compareTo));
  private @Nullable MolecularStructure structure;

  public SimpleCompoundDBAnnotation() {
  }

  public SimpleCompoundDBAnnotation(final String formula) {
    setFormula(formula);
  }

  public static CompoundDBAnnotation loadFromXML(XMLStreamReader reader,
      @NotNull final MZmineProject project, ModularFeatureList flist, ModularFeatureListRow row)
      throws XMLStreamException {
    final String startElementName = reader.getLocalName();
    final String startElementAttrValue = Objects.requireNonNullElse(
        reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE_OLD),
        reader.getAttributeValue(null, XML_TYPE_ATTR));

    if (!((reader.isStartElement() && startElementName.equals(XML_ELEMENT_OLD) // old case
           && startElementAttrValue.equals(XML_TYPE_NAME_OLD))                   // old case
          || (reader.isStartElement() && startElementName.equals(FeatureAnnotation.XML_ELEMENT)
              && startElementAttrValue.equals(XML_ATTR)))) {
      throw new IllegalStateException("Invalid xml element to load CompoundDBAnnotation from.");
    }

    final SimpleCompoundDBAnnotation id = new SimpleCompoundDBAnnotation();
    final int numEntries = Integer.parseInt(reader.getAttributeValue(null, XML_NUM_ENTRIES_ATTR));

    int i = 0;
    while (reader.hasNext() && !(reader.isEndElement() && (
        reader.getLocalName().equals(XML_ELEMENT_OLD) || reader.getLocalName()
            .equals(XML_ELEMENT)))) {
      reader.next();

      if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        continue;
      }

      final DataType typeForId = DataTypes.getTypeForId(
          reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
      if (typeForId != null) {
        Object o = typeForId.loadFromXML(reader, project, flist, row, null, null);
        id.put(typeForId, o);
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

  /**
   * Calculate neutral mass if not already present. then keep the original.
   *
   * @param formula molecular formula
   */
  public void setFormula(final String formula) {
    putIfNotNull(FormulaType.class, formula);

    final IMolecularFormula neutralFormula = FormulaUtils.neutralizeFormulaWithHydrogen(formula);
    if (neutralFormula != null) {
      put(NeutralMassType.class, MolecularFormulaManipulator.getMass(neutralFormula,
          MolecularFormulaManipulator.MonoIsotopic));
    }
  }

  /**
   * @return the structure parsed from smiles or inchi
   */
  @Override
  public MolecularStructure getStructure() {
    if (structure != null) {
      return structure;
    }
    String smiles = getSmiles();
    String inchi = getInChI();
    structure = StructureParser.silent().parseStructure(smiles, inchi);
    return structure;
  }

  @Override
  public void setStructure(final MolecularStructure structure) {
    if (structure == null) {
      return;
    }
    putIfNotNull(MolecularStructureType.class, structure);
    putIfNotNull(SmilesStructureType.class, structure.canonicalSmiles());
    putIfNotNull(SmilesIsomericStructureType.class, structure.isomericSmiles());
    putIfNotNull(InChIKeyStructureType.class, structure.inchiKey());
    putIfNotNull(InChIStructureType.class, structure.inchi());
    putIfNotNull(FormulaType.class, structure.formulaString());
    putIfNotNull(NeutralMassType.class, structure.monoIsotopicMass());
  }

  @Override
  public <T> T get(@NotNull DataType<T> key) {
    // this type is not in the map to avoid export. It is calculated on demand
    if (key instanceof MolecularStructureType) {
      return (T) getStructure();
    }

    Object value = data.get(key);
    if (value != null && !key.getValueClass().isInstance(value)) {
      throw new IllegalStateException(
          String.format("Value type (%s) does not match data type value class (%s)",
              value.getClass(), key.getValueClass()));
    }
    return (T) value;
  }

  @Override
  public <T> T get(Class<? extends DataType<T>> key) {
    var actualKey = DataTypes.get(key);
    return get(actualKey);
  }

  @Override
  public <T> T put(@NotNull DataType<T> key, T value) {
    if (value == null) {
      return (T) data.remove(key);
    }

    if (!key.getValueClass().isInstance(value)) {
      throw new IllegalArgumentException(
          String.format("Cannot put value class (%s) for data type (%s). Value type mismatch.",
              value.getClass(), key.getClass()));
    }
    var actualKey = DataTypes.get(key);
    return (T) data.put(actualKey, value);
  }

  @Override
  public <T> T put(@NotNull Class<? extends DataType<T>> key, T value) {
    var actualKey = DataTypes.get(key);
    if (value == null) {
      return (T) data.remove(actualKey);
    }

    if (!actualKey.getValueClass().isInstance(value)) {
      throw new IllegalArgumentException(
          String.format("Cannot put value class (%s) for data type (%s). Value type mismatch.",
              value.getClass(), actualKey.getClass()));
    }
    return (T) data.put(actualKey, value);
  }

  @Override
  public Set<DataType> getTypes() {
    return data.keySet();
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
    writeOpeningTag(writer);
    writer.writeAttribute(CompoundDBAnnotation.XML_NUM_ENTRIES_ATTR, String.valueOf(data.size()));

    for (Entry<DataType, Object> entry : data.entrySet()) {
      final DataType key = entry.getKey();
      final Object value = entry.getValue();

      try {
        writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
        writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, key.getUniqueID());
        key.saveToXML(writer, value, flist, row, null, null);
        writer.writeEndElement();
      } catch (XMLStreamException e) {
        final Object finalVal = value;
        logger.warning(
            () -> "Error while writing data type " + key.getClass().getSimpleName() + " with value "
                  + finalVal + " to xml.");
        e.printStackTrace();
      }
    }

    writeClosingTag(writer);
  }

  @Override
  public boolean matches(FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobilityTolerance,
      @Nullable Double percentCCSTolerance) {

    final Double exactMass = getPrecursorMZ();
    // values are "matched" if the given value exists in this class and falls within the tolerance.
    if (mzTolerance != null && exactMass != null && (row.getAverageMZ() == null
                                                     || !mzTolerance.checkWithinTolerance(
        row.getAverageMZ(), exactMass))) {
      return false;
    }

    // values <=0 are wildcards and always match because they are invalid. see documentation
    final Float rt = getRT();
    if (rtTolerance != null && rt != null && rt > 0 && (row.getAverageRT() == null
                                                        || !rtTolerance.checkWithinTolerance(
        row.getAverageRT(), rt))) {
      return false;
    }

    // values <=0 are wildcards and always match because they are invalid. see documentation
    final Float mobility = getMobility();
    if (mobilityTolerance != null && mobility != null && mobility > 0 && (
        row.getAverageMobility() == null || !mobilityTolerance.checkWithinTolerance(mobility,
            row.getAverageMobility()))) {
      return false;
    }

    // values <=0 are wildcards and always match because they are invalid. see documentation
    final Float ccs = getCCS();
    return percentCCSTolerance == null || ccs == null || ccs <= 0 || (row.getAverageCCS() != null
                                                                      && !(
        Math.abs(1 - (row.getAverageCCS() / ccs)) > percentCCSTolerance));
  }

  @Override
  public Map<DataType, Object> getReadOnlyMap() {
    return Collections.unmodifiableMap(data);
  }

  @Override
  public CompoundDBAnnotation clone() {
    SimpleCompoundDBAnnotation clone = new SimpleCompoundDBAnnotation();
    data.forEach((key, value) -> clone.put(key, value));
    return clone;
  }

  @Override
  public String toString() {
    return Stream.of(getCompoundName(), getAdductType(), getScoreString()).filter(Objects::nonNull)
        .map(Objects::toString).collect(Collectors.joining(": "));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleCompoundDBAnnotation that)) {
      return false;
    }
    return Objects.equals(data, that.data);
  }

  @Override
  public @NotNull String getXmlAttributeKey() {
    return XML_ATTR;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }
}

