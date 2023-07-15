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

package io.github.mzmine.datamodel.features.compoundannotations;

import static io.github.mzmine.util.ParsingUtils.readNullableString;

import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Define and add new databases to {@link #getDatabases()}
 *
 * @param shortName   short name is used most often
 * @param name        full name
 * @param idHeader    this header is used in several places for IDs
 * @param description short description of the DB
 * @param url         url to website. Use ID_PLACEHOLDER if this URL points towards entries
 */
public record Database(@NotNull String shortName, @NotNull String name, @NotNull String idHeader,
                       @Nullable String description, @Nullable String url) {

  public static final String XML_ELEMENT = "database";
  public static final String ID_PLACEHOLDER = "ID_PLACEHOLDER";


  public Database(@NotNull final String shortName, @Nullable final String description,
      @Nullable final String url) {
    this(shortName, shortName, shortName.toLowerCase() + "_id", description, url);
  }

  public Database(@NotNull final String shortName, @Nullable final String name) {
    this(shortName, name, null);
  }

  public Database(@NotNull final String shortName) {
    this(shortName, null);
  }

  public static final Database UNKNOWN = new Database("UnknownDB");
  public static final Database PUBCHEM = new Database("PubChem", "PubChem", "pubchem_cid", null,
      null);
  public static final Database CHEM_SPIDER = new Database("ChemSpider");
  public static final Database CHEBI = new Database("ChEBI");
  public static final Database CHEMBL = new Database("ChEMBL");
  public static final Database METACYC = new Database("MetaCyc");
  public static final Database KEGG = new Database("KEGG");
  public static final Database HMDB = new Database("HMDB", "Human Metabolome DB");
  public static final Database YMDB = new Database("YMDB", "Yeast Metabolome DB");
  public static final Database LIPID_MAPS = new Database("LipidMaps");

  // drugs
  public static final Database DRUGBANK = new Database("Drugbank");
  public static final Database DRUG_CENTRAL = new Database("DrugCentral");
  public static final Database UNII = new Database("UNII", "UNII", "unii", null, null);

  // spectral
  public static final Database MZMINE_SPECTRA = new Database("MZmineSpec");
  public static final Database GNPS = new Database("GNPS");
  public static final Database MZCLOUD = new Database("MZCLOUD");
  public static final Database METLIN = new Database("METLIN");
  public static final Database USI = new Database("USI", "Universal Spectrum Identifier", "usi",
      "Link to public spectra", null);
  public static final Database MASSBANK_EU = new Database("MassBank.eu", "MassBank Europe");
  public static final Database MASSBANK_NA = new Database("MoNA", "MassBank of North America");
  public static final Database MASSBANK_JP = new Database("MassBank.jp", "MassBank Japan");

  // repository
  public static final Database MASSIVE = new Database("MassIVE");
  public static final Database METABOLIGHTS = new Database("MetaboLights");
  public static final Database METABOLOMICS_WORKBENCH = new Database("MetabolomicsWorkbench");

  // publications
  public static final Database PUBMED = new Database("PUBMED");

  private static final List<Database> databases = new ArrayList<>(
      List.of(UNKNOWN, PUBCHEM, CHEM_SPIDER, CHEBI, CHEMBL, METACYC, KEGG, HMDB, YMDB, LIPID_MAPS,
          // drug
          DRUGBANK, DRUG_CENTRAL, UNII,
          // spectra
          MZMINE_SPECTRA, MZCLOUD, GNPS, METLIN, USI, MASSBANK_EU, MASSBANK_JP, MASSBANK_NA,
          // datasets
          MASSIVE, METABOLIGHTS, METABOLOMICS_WORKBENCH,
          // publications
          PUBMED));

  /**
   * Mutable list of databases. Add new entries to this list
   *
   * @return list of databases
   */
  public static List<Database> getDatabases() {
    return databases;
  }

  public static Database addDatabase(Database db) {
    databases.add(db);
    return db;
  }

  @NotNull
  public static Database getForShortName(@Nullable String shortName) {
    if(shortName==null)
      return UNKNOWN;
    return databases.stream().filter(db -> db.shortName.equals(shortName)).findFirst()
        .orElseGet(() -> addDatabase(new Database(shortName)));
  }

  @NotNull
  public static Database getForDatabaseIdHeader(@Nullable String idHeader) {
    if(idHeader==null)
      return UNKNOWN;
    return databases.stream().filter(db -> db.idHeader.equals(idHeader)).findFirst()
        .orElseGet(() -> addDatabase(new Database(idHeader, idHeader, idHeader, null, null)));
  }

  @Nullable
  public static Database forField(final DBEntryField field) {
    return switch (field) {
      case ENTRY_ID, NAME, SYNONYMS, COMMENT, DESCRIPTION, MOLWEIGHT, EXACT_MASS, //
          FORMULA, INCHI, INCHIKEY, SMILES, ISOMERIC_SMILES, PEPTIDE_SEQ, QUALITY, //
          QUALITY_PRECURSOR_PURITY, QUALITY_CHIMERIC, QUALITY_EXPLAINED_INTENSITY, //
          QUALITY_EXPLAINED_SIGNALS, OTHER_MATCHED_COMPOUNDS_N, OTHER_MATCHED_COMPOUNDS_NAMES, //
          NUM_PEAKS, UNSPECIFIED, MS_LEVEL, RT, CCS, ION_TYPE, PRECURSOR_MZ, CHARGE, //
          MERGED_SPEC_TYPE, SIRIUS_MERGED_SCANS, SIRIUS_MERGED_STATS, COLLISION_ENERGY, //
          FRAGMENTATION_METHOD, ISOLATION_WINDOW, ACQUISITION, MSN_COLLISION_ENERGIES, //
          MSN_PRECURSOR_MZS, MSN_FRAGMENTATION_METHODS, MSN_ISOLATION_WINDOWS, INSTRUMENT_TYPE,//
          INSTRUMENT, ION_SOURCE, RESOLUTION, POLARITY, PRINCIPAL_INVESTIGATOR, DATA_COLLECTOR, //
          SOFTWARE, DATASET_ID, FILENAME, USI, SCAN_NUMBER, SPLASH, CAS, FEATURE_ID -> null;
      // identifier
      case PUBMED -> PUBMED;
      case PUBCHEM -> PUBCHEM;
      case CHEBI -> CHEBI;
      case CHEMBL -> CHEMBL;
      case HMDB -> HMDB;
      case LIPIDMAPS -> LIPID_MAPS;
      case KEGG -> KEGG;
      case UNII -> UNII;
      case DRUGBANK -> DRUGBANK;
      case DRUGCENTRAL -> DRUG_CENTRAL;
      case GNPS_ID -> GNPS;
      case MONA_ID -> MASSBANK_NA;
      case CHEMSPIDER -> CHEM_SPIDER;
    };
  }


  @Nullable
  public String getUrl(final String id) {
    if (url == null) {
      return null;
    }
    return url.replace(ID_PLACEHOLDER, id);
  }

  public void saveToXML(final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeAttribute("short_name", shortName());
    writer.writeAttribute("name", name());
    writer.writeAttribute("id_header", idHeader());
    writer.writeAttribute("description", ParsingUtils.parseNullableString(description()));
    writer.writeAttribute("url", ParsingUtils.parseNullableString(url()));

    writer.writeEndElement(); // this entry
  }


  public static Database loadFromXML(final XMLStreamReader reader) throws XMLStreamException {
    final String shortName = reader.getAttributeValue(null, "short_name");
    final String name = reader.getAttributeValue(null, "name");
    final String idHeader = reader.getAttributeValue(null, "id_header");
    final String descr = readNullableString(reader.getAttributeValue(null, "description"));
    final String url = readNullableString(reader.getAttributeValue(null, "url"));

    final Database db = new Database(shortName, name, idHeader, descr, url);
    return databases.stream().filter(original -> Objects.equals(original, db)).findFirst()
        .orElse(db);
  }
}
