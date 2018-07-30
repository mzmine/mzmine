/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.db;

import java.net.MalformedURLException;
import java.net.URL;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.KEGGGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.LipidMapsGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.PubChemGateway;

public class SiriusDBCompound extends SimplePeakIdentity {
  private final static String PUBCHEM = "PubChem";
  private final static String KEGG = "KEGG";
  private final static String CHEBI = "CHEBI";
  private final static String PLANTCYC = "Plantcyc";
  private final static String YMDB = "YMDB";
  private final static String BIOCYC = "Biocyc";
  private final static String KNAPSACK = "KNApSAcK";
  private final static String HMDB = "HMDB";
  private final static String LIPID_MAPS = "Lipid Maps"; //todo: not checked!

  private final static String PUBCHEM_ENTRY = PubChemGateway.pubchemEntryAddress;
  private final static String KEGG_ENTRY = KEGGGateway.keggEntryAddress;
  private final static String HMDB_ENTRY = "http://www.hmdb.ca/metabolites/HMDB0000000";
  private final static String YMDB_ENTRY = "http://www.ymdb.ca/compounds/YMDB00000";
  private final static String LIPID_MAPS_ENTRY = LipidMapsGateway.lipidMapsEntryAddress;
  private final static String CHEBI_ENTRY = "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:";
  private final static String KNAPSACK_ENTRY = "http://kanaya.naist.jp/knapsack_jsp/information.jsp?word=C00000000";
  private final static String PLANTCYC_ENTRY = "https://pmn.plantcyc.org/compound?orgid=PLANT&id=";
  private final static String BIOCYC_ENTRY = "https://biocyc.org/compound?orgid=META&id=";

  private final String db;
  private final String id;

  public SiriusDBCompound(String DB, String ID) {
    this.db = DB;
    this.id = ID;
  }

  public String getID() {
    return id;
  }

  public String getDB() {
    return db;
  }

  public URL generateURL() throws MalformedURLException {
    String entry = "";
    int symbols = id.length();
    switch(db) {
      case PUBCHEM:
        entry = PUBCHEM_ENTRY;
        break;
      case KEGG:
        entry = KEGG_ENTRY;
        break;
      case CHEBI:
        entry = CHEBI_ENTRY;
        break;
      case YMDB:
        entry = YMDB_ENTRY;
        entry = entry.substring(0, entry.length() - symbols);
        break;
      case LIPID_MAPS:
        entry = LIPID_MAPS_ENTRY;
        break;
      case HMDB:
        entry = HMDB_ENTRY;
        entry = entry.substring(0, entry.length() - symbols);
        break;
      case KNAPSACK:
        entry = KNAPSACK_ENTRY;
        entry = entry.substring(0, entry.length() - symbols);
        break;
      case PLANTCYC:
        entry = PLANTCYC_ENTRY;
        break;
      case BIOCYC:
        entry = BIOCYC_ENTRY;
        break;


      default:
        return null;
    }

    entry += id;
    return new URL(entry);
  }

}
