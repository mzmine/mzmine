/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport;

import java.text.MessageFormat;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identity of GNPS library matching results imported from a graphml network
 */
public class GNPSLibraryMatch {

  private final String identity;
  private final HashMap<String, Object> results;

  public GNPSLibraryMatch(@NotNull HashMap<String, Object> results, String compound) {
    this.results = results;
    String adduct = getResultOr(ATT.ADDUCT, "").toString();
    String score = getResultOr(ATT.LIBRARY_MATCH_SCORE, "NO_SCORE").toString();
    identity = MessageFormat.format("{0}; {1}; cos={2}", compound, adduct, score);
  }

  @NotNull
  public HashMap<String, Object> getResults() {
    return results;
  }

  /**
   * Retrieve results for a specific attribute
   *
   * @param att the attribute to retrieve a field
   * @return the result for this attribute or null
   */
  @Nullable
  public Object getResult(ATT att) {
    return results.get(att.getKey());
  }

  /**
   * Get result or return default
   *
   * @param att          the attribute to retrieve a field
   * @param defaultValue the value to replace null
   * @return if result is null return default
   */
  @NotNull
  public <T> T getResultOr(ATT att, @NotNull T defaultValue) {
    Object result = getResult(att);
    return result == null ? defaultValue : (T) result;
  }

  @Override
  public String toString() {
    return identity;
  }

  /**
   * Different node attributes from GNPS FBMN or IIMN
   */
  public enum ATT {
    CLUSTER_INDEX("cluster index", Integer.class), // GNPS cluster -
    // similarity
    COMPOUND_NAME("Compound_Name", String.class), // library match
    ADDUCT("Adduct", String.class), // from GNPS library match
    MASS_DIFF("MassDiff", Double.class), // absolute diff from gnps
    LIBRARY_MATCH_SCORE("MQScore", Double.class), // cosine score to library
    // spec
    SHARED_SIGNALS("SharedPeaks", String.class), // shared signals library
    // <-> query
    INCHI("INCHI", String.class), SMILES("Smiles", String.class), // structures
    IONSOURCE("Ion_Source", String.class), IONMODE("IonMode",
        String.class), INSTRUMENT("Instrument", String.class), // instrument
    GNPS_LIBRARY_URL("GNPSLibraryURL", String.class), // link to library
    // entry
    GNPS_NETWORK_URL("GNPSLinkout_Network", String.class), // link to
    // network
    GNPS_CLUSTER_URL("GNPSLinkout_Cluster", String.class); // link to
    // cluster

    private final String key;
    private final Class c;

    ATT(String key, Class c) {
      this.c = c;
      this.key = key;
    }

    public Class getValueClass() {
      return c;
    }

    public String getKey() {
      return key;
    }
  }
}
