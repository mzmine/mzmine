/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MolecularStructureType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spectral library entry is a mass spectrum that can be memory mapped (memory map defined in
 * {@link SpectralLibrary}.
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public interface SpectralLibraryEntry extends MassList {

  Logger logger = Logger.getLogger(SpectralLibraryEntry.class.getName());
  String XML_ELEMENT_ENTRY = "spectraldatabaseentry";

  static SpectralLibraryEntry loadFromXML(XMLStreamReader reader, MZmineProject project)
      throws XMLStreamException {
    return SpectralDBEntry.loadFromXML(reader, project);
  }

  void putAll(Map<DBEntryField, Object> fields);

  /**
   * @return True if the value was not null and the value was placed in the field.
   */
  boolean putIfNotNull(DBEntryField field, Object value);

  Double getPrecursorMZ();

  Optional<Object> getField(DBEntryField f);

  <T> T getOrElse(DBEntryField f, T defaultValue);

  Map<DBEntryField, Object> getFields();


  /**
   * Value for this datatype
   * <p>
   * In preparation to extend {@link ModularDataModel} in the future
   *
   * @return value of this type or null
   */
  @Nullable
  default <T> T get(Class<? extends DataType<T>> tclass) {
    DataType<T> type = DataTypes.get(tclass);
    return get(type);
  }

  /**
   * Value for this datatype
   * <p>
   * In preparation to extend {@link ModularDataModel} in the future
   *
   * @return value of this type or null
   */
  @Nullable
  default <T extends Object> T get(DataType<T> type) {
    // handle special types that may have values in various object types
    // securely handle the conversion to T
    final T value = switch (type) {
      // need to handle both IonTypeType and IonAdductType because spectral match still used outdated IonAdductType
      case IonTypeType _ -> {
        final Object v = getField(DBEntryField.ION_TYPE).orElse(null);
        if (v == null) {
          yield null;
        }
        if (v instanceof IonType ion) {
          yield (T) ion;
        }
        try {
          final IonType ion = IonTypeParser.parse(v.toString());
          yield (T) ion;
        } catch (Exception e) {
          yield null;
        }
      }
      case IonAdductType _ ->
          (T) getField(DBEntryField.ION_TYPE).map(Object::toString).orElse(null);
      case MolecularStructureType _ -> (T) getStructure();
      default -> null;
    };
    if (value != null) {
      return value;
    }

    // find regular fields and try to map them to T
    final DBEntryField field = DBEntryField.fromDataType(type);
    if (field == null || field == DBEntryField.UNSPECIFIED) {
      return null;
    }
    try {

      // try to cast and if it fails then return null because those values are loaded from different sources
      // and some fields may have different types
      return (T) getOrElse(field, null);
    } catch (Exception e) {
      return null;
    }
  }


  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  default boolean setCharge(@Nullable Integer charge) {
    return setCharge(charge, PolarityType.fromInt(charge));
  }

  /**
   * Sets the charge and the polarity of this entry. The polarity overrides any +/- in the charge
   * integer. Sets an integer charge
   *
   * @return {@link #putIfNotNull(DBEntryField, Object)}
   */
  default boolean setCharge(@Nullable Integer charge, @Nullable PolarityType polarity) {
    if (charge == null) {
      return false;
    }
    if (PolarityType.isDefined(polarity)) {
      return putIfNotNull(DBEntryField.CHARGE, Math.abs(charge) * polarity.getSign());
    } else {
      // previously this would set charge as 1- but we actually expect charge to be integer
      return putIfNotNull(DBEntryField.CHARGE, charge);
    }
  }

  /**
   * Extract MS level from scan
   */
  @NotNull
  default Optional<Integer> getMsLevel() {
    return getAsInteger(DBEntryField.MS_LEVEL);
  }

  /**
   * Extract MS level from scan
   *
   * @return null if none provided
   */
  @Nullable
  default Double getPrecursorMz() {
    return getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null);
  }

  @NotNull
  default PolarityType getPolarity() {
    return getField(DBEntryField.POLARITY).map(Object::toString).map(PolarityType::parseFromString)
        .orElse(PolarityType.UNKNOWN);
  }

  default Optional<Float> getAsFloat(DBEntryField field) {
    try {
      return getField(field).map(this::toFloat);
    } catch (Exception ex) {
      logger.finest("Cannot convert to float " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Float toFloat(Object v) {
    return switch (v) {
      case Number n -> n.floatValue();
      case String s -> Float.parseFloat(s);
      default -> null;
    };
  }

  default Optional<String> getAsString(DBEntryField field) {
    return getField(field).map(Object::toString);
  }

  default Optional<Double> getAsDouble(DBEntryField field) {
    try {
      return getField(field).map(this::toDouble);
    } catch (Exception ex) {
      logger.finest("Cannot convert to double " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Double toDouble(Object v) {
    return switch (v) {
      case Number n -> n.doubleValue();
      case String s -> Double.parseDouble(s);
      default -> null;
    };
  }

  default Optional<Integer> getAsInteger(DBEntryField field) {
    try {
      return getField(field).map(this::toInteger);
    } catch (Exception ex) {
      logger.finest("Cannot convert to integer " + field.toString() + " value: " + getField(field));
      return Optional.empty();
    }
  }

  default Integer toInteger(Object v) {
    return switch (v) {
      case Number n -> n.intValue();
      case String s -> Integer.parseInt(s);
      default -> null;
    };
  }

  @Nullable SpectralLibrary getLibrary();

  void setLibrary(@Nullable SpectralLibrary library);

  @Nullable String getLibraryName();

  /**
   * @return Formula from the entered formula or from the structure if no formula provided
   */
  @Nullable String getFormula();

  /**
   * @return ion type
   */
  @Nullable IonType getAdductType();

  /**
   * @return the structure parsed from smiles or inchi
   */
  MolecularStructure getStructure();

  /**
   * Isotope pattern is cached and only calculated once on demand. Modules may already calculate the
   * isotope pattern to speed up later use in tables.
   *
   * @return the isotope pattern of the ion formula
   */
  @Nullable IsotopePattern getIsotopePattern();
}
