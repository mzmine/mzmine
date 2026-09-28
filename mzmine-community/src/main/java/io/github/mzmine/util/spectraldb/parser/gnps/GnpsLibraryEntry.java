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

package io.github.mzmine.util.spectraldb.parser.gnps;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * One entry of a GNPS json library. Covers both flavors that GNPS publishes:
 * <ul>
 *   <li>the classic export, which writes every key of the submission form</li>
 *   <li>the cleaned libraries, which rename several keys, write every value as a string and put
 *       the compound classes in</li>
 * </ul>
 * Both are a json array of flat objects with the signals in a peaks_json string, so the aliases
 * below are enough to read them with one parser. Numbers are read as strings and converted here
 * because the cleaned libraries write things like a charge of "1.0" and use placeholder words
 * where a value is missing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record GnpsLibraryEntry(
    // entry specific
    @JsonAlias("SpectrumID") String spectrum_id, String splash, String ms_level, String Ion_Mode,
    String Adduct, String Precursor_MZ, String ExactMass, String Charge,

    @JsonProperty("peaks_json") @JsonDeserialize(using = SpectrumDeserializer.class) double[][] spectrum,

    // compound specific
    String Compound_Name, String Compound_Source, String CAS_Number, String Pubmed_ID,
    // structures
    String Smiles, String INCHI,
    /**
     * The classic export always writes "N/A" here and carries the real key in
     * {@link #InChIKey_smiles}, so both are read and the usable one wins.
     */
    String INCHI_AUX, String InChIKey_smiles,

    // instrument specific, the cleaned libraries use the ms* names
    @JsonAlias("msIonisation") String Ion_Source,
    @JsonAlias({"msMassAnalyzer", "msManufacturer"}) String Instrument,
    @JsonProperty("msDissociationMethod") String fragmentationMethod,
    @JsonProperty("collision_energy") String collisionEnergy,

    // compound classes, cleaned libraries only
    String classyfire_superclass, String classyfire_class, String classyfire_subclass,
    String classyfire_direct_parent,

    // contacts
    @JsonProperty("Data_Collector") String dataCollector,
    @JsonProperty("PI") String principalInvestigator) implements GnpsEntry {

  /**
   * Words the GNPS flavours use where a value is missing. Compared lower case.
   */
  private static final Set<String> PLACEHOLDERS = Set.of("n/a", "na", "nan", "none", "null", "-");

  /**
   * @return the value, or null if it is blank or one of the {@link #PLACEHOLDERS}
   */
  @Nullable
  private static String clean(@Nullable final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    final String trimmed = value.trim();
    return PLACEHOLDERS.contains(trimmed.toLowerCase()) ? null : trimmed;
  }

  /**
   * @return the first usable value or null
   */
  @Nullable
  private static String firstNotNull(@Nullable final String a, @Nullable final String b) {
    final String cleanedA = clean(a);
    return cleanedA != null ? cleanedA : clean(b);
  }

  /**
   * The cleaned libraries write whole numbers with a decimal point, for example a charge of "1.0",
   * so this parses as a double and rounds.
   *
   * @return the value as an integer or null if it is missing or unparsable
   */
  @Nullable
  private static Integer toInteger(@Nullable final String value) {
    final Double number = toDouble(value);
    return number == null ? null : (int) Math.round(number);
  }

  /**
   * @return the value as a double or null if it is missing or unparsable
   */
  @Nullable
  private static Double toDouble(@Nullable final String value) {
    final String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    try {
      final double parsed = Double.parseDouble(cleaned);
      return Double.isFinite(parsed) ? parsed : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public SpectralLibraryEntry toSpectralLibraryEntry(@Nullable SpectralLibrary library) {
    MemoryMapStorage storage = library == null ? null : library.getStorage();
    SpectralDBEntry entry = new SpectralDBEntry(storage, spectrum[0], spectrum[1]);
    entry.putIfNotNull(DBEntryField.ENTRY_ID, clean(spectrum_id));
    entry.putIfNotNull(DBEntryField.GNPS_ID, clean(spectrum_id));
    entry.putIfNotNull(DBEntryField.SPLASH, clean(splash));
    entry.putIfNotNull(DBEntryField.MS_LEVEL, toInteger(ms_level));
    entry.putIfNotNull(DBEntryField.NAME, clean(Compound_Name));
    entry.putIfNotNull(DBEntryField.ION_TYPE, clean(Adduct));
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, toDouble(Precursor_MZ));
    entry.putIfNotNull(DBEntryField.EXACT_MASS, toDouble(ExactMass));
    entry.putIfNotNull(DBEntryField.CHARGE, toInteger(Charge));
    entry.putIfNotNull(DBEntryField.DATA_COLLECTOR, clean(dataCollector));
    entry.putIfNotNull(DBEntryField.PRINCIPAL_INVESTIGATOR, clean(principalInvestigator));
    entry.putIfNotNull(DBEntryField.ACQUISITION, clean(Compound_Source));
    entry.putIfNotNull(DBEntryField.CAS, clean(CAS_Number));
    entry.putIfNotNull(DBEntryField.PUBMED, clean(Pubmed_ID));
    entry.putIfNotNull(DBEntryField.INCHI, clean(INCHI));
    entry.putIfNotNull(DBEntryField.INCHIKEY, firstNotNull(INCHI_AUX, InChIKey_smiles));
    entry.putIfNotNull(DBEntryField.SMILES, clean(Smiles));
    entry.putIfNotNull(DBEntryField.INSTRUMENT_TYPE, clean(Instrument));
    entry.putIfNotNull(DBEntryField.ION_SOURCE, clean(Ion_Source));
    entry.putIfNotNull(DBEntryField.POLARITY, clean(Ion_Mode));
    entry.putIfNotNull(DBEntryField.FRAGMENTATION_METHOD, clean(fragmentationMethod));
    entry.putIfNotNull(DBEntryField.COLLISION_ENERGY, clean(collisionEnergy));
    entry.putIfNotNull(DBEntryField.CLASSYFIRE_SUPERCLASS, clean(classyfire_superclass));
    entry.putIfNotNull(DBEntryField.CLASSYFIRE_CLASS, clean(classyfire_class));
    entry.putIfNotNull(DBEntryField.CLASSYFIRE_SUBCLASS, clean(classyfire_subclass));
    entry.putIfNotNull(DBEntryField.CLASSYFIRE_PARENT, clean(classyfire_direct_parent));
    return entry;
  }

}
