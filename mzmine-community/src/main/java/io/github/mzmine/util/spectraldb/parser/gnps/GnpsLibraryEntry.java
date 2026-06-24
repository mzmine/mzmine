/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import org.jetbrains.annotations.Nullable;


@JsonIgnoreProperties(ignoreUnknown = true)
record GnpsLibraryEntry(
    // entry specific
    String spectrum_id, String splash, int ms_level, String Ion_Mode, String Adduct,
    double Precursor_MZ, double ExactMass, int Charge,

    @JsonProperty("peaks_json") @JsonDeserialize(using = SpectrumDeserializer.class) double[][] spectrum,

    // compound specific
    String Compound_Name, String Compound_Source, String CAS_Number, String Pubmed_ID,
    // structures
    String Smiles, String INCHI, String INCHI_AUX,

    // instrument specific
    String Ion_Source, String Instrument,
    // contacts
    @JsonProperty("Data_Collector") String dataCollector,
    @JsonProperty("PI") String principalInvestigator) {

  public SpectralLibraryEntry toSpectralLibraryEntry(@Nullable SpectralLibrary library) {
    MemoryMapStorage storage = library == null ? null : library.getStorage();
    SpectralDBEntry entry = new SpectralDBEntry(storage, spectrum[0], spectrum[1]);
    entry.putIfNotNull(DBEntryField.ENTRY_ID, spectrum_id);
    entry.putIfNotNull(DBEntryField.GNPS_ID, spectrum_id);
    entry.putIfNotNull(DBEntryField.SPLASH, splash);
    entry.putIfNotNull(DBEntryField.MS_LEVEL, ms_level);
    entry.putIfNotNull(DBEntryField.NAME, Compound_Name);
    entry.putIfNotNull(DBEntryField.ION_TYPE, Adduct);
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, Precursor_MZ);
    entry.putIfNotNull(DBEntryField.EXACT_MASS, ExactMass);
    entry.putIfNotNull(DBEntryField.CHARGE, Charge);
    entry.putIfNotNull(DBEntryField.DATA_COLLECTOR, dataCollector);
    entry.putIfNotNull(DBEntryField.PRINCIPAL_INVESTIGATOR, principalInvestigator);
    entry.putIfNotNull(DBEntryField.ACQUISITION, Compound_Source);
    entry.putIfNotNull(DBEntryField.CAS, CAS_Number);
    entry.putIfNotNull(DBEntryField.PUBMED, Pubmed_ID);
    entry.putIfNotNull(DBEntryField.INCHI, INCHI);
    entry.putIfNotNull(DBEntryField.INCHIKEY, INCHI_AUX);
    entry.putIfNotNull(DBEntryField.SMILES, Smiles);
    entry.putIfNotNull(DBEntryField.INSTRUMENT_TYPE, Instrument);
    entry.putIfNotNull(DBEntryField.ION_SOURCE, Ion_Source);
    entry.putIfNotNull(DBEntryField.POLARITY, Ion_Mode);
    return entry;
  }

}
