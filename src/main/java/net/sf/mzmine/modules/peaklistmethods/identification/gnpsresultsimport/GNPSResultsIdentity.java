/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport;

import java.text.MessageFormat;
import java.util.HashMap;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

/**
 * Identity of GNPS library matching results import.
 */
public class GNPSResultsIdentity extends SimplePeakIdentity {
  public enum ATT {
    CLUSTER_INDEX("cluster index", Integer.class), // GNPS cluster - similarity
    COMPOUND_NAME("Compound_Name", String.class), // library match
    ADDUCT("Adduct", String.class), // from GNPS library match
    MASS_DIFF("MassDiff", Double.class), // absolute diff from gnps
    LIBRARY_MATCH_SCORE("MQScore", Double.class), // cosine score to library spec
    SHARED_SIGNALS("SharedPeaks", String.class), // shared signals library <-> query
    INCHI("INCHI", String.class), SMILES("Smiles", String.class), // structures
    IONSOURCE("Ion_Source", String.class), IONMODE("IonMode",
        String.class), INSTRUMENT("Instrument", String.class), // instrument
    GNPS_LIBRARY_URL("GNPSLibraryURL", String.class), // link to library entry
    GNPS_NETWORK_URL("GNPSLinkout_Network", String.class), // link to network
    GNPS_CLUSTER_URL("GNPSLinkout_Cluster", String.class); // link to cluster

    private String key;
    private Class c;

    private ATT(String key, Class c) {
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

  private HashMap<String, Object> results;

  public GNPSResultsIdentity(HashMap<String, Object> results, String compound, String adduct) {
    super(MessageFormat.format("{0} ({1})", compound, adduct));
    this.results = results;
    setPropertyValue(PROPERTY_METHOD, "GNPS results import");
    setPropertyValue(PROPERTY_URL, results.get(ATT.GNPS_LIBRARY_URL.getKey()).toString());
  }

  public HashMap<String, Object> getResults() {
    return results;
  }

  public Object getResult(ATT att) {
    return results.get(att.toString());
  }
}
