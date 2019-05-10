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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectralMatchTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

// main:
// -- compound: inchi, inchiKey,
// -- -- names: name
// -- -- metaData.name/ value: SMILES, molecular formula, total exact mass, pubchem cid
// -- -- molFile
// -- -- classification
// -- id (mona id)
// -- metaData: (array of name / value)
// -- -- accession (mona id)
// -- -- author
// -- -- license
// -- -- copyright
// -- -- publication
// -- -- exact mass
// -- -- instrument
// -- -- instrument type
// -- -- ms level
// -- -- collision energy
// -- -- ionization
// -- -- resolution
// -- -- column
// -- -- retention time (xxx sec
// -- -- precursor m/z
// -- -- precursor type (adduct)
// -- -- ionization mode (positive / negative)
// -- -- mass accuracy (name, unit, value)
// -- -- mass error (name, unit, value)
// -- spectrum
// -- -- 100:2 101:15 102:10
// -- splash
// -- submitter: id, emailAddress, firstName, lastName, institution

// top level json objects/arrays
/**
 * Mass bank of North America (MONA) json database files
 * 
 * @author Robin Schmid
 *
 */
public class MonaJsonParser implements SpectralDBParser {

  private static final String COMPOUND = "compound", MONA_ID = "id", META_DATA = "metaData",
      SPECTRUM = "spectrum", SPLASH = "splash", SUBMITTER = "submitter";

  private static Logger logger = Logger.getLogger(MonaJsonParser.class.getName());

  @Override
  public @Nonnull List<SpectralMatchTask> parse(AbstractTask mainTask, ParameterSet parameters,
      File dataBaseFile, SpectraPlot spectraPlot, Scan scan) throws IOException {
    logger.info("Parsing MONA spectral json library " + dataBaseFile.getAbsolutePath());
    List<SpectralMatchTask> tasks = new ArrayList<>();
    List<SpectralDBEntry> list = new ArrayList<>();

    int error = 0;
    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null;) {
        // main task was canceled?
        if (mainTask.isCanceled()) {
          for (SpectralMatchTask t : tasks)
            t.cancel();
          return new ArrayList<>();
        }

        JsonReader reader = null;
        try {
          reader = Json.createReader(new StringReader(l));
          JsonObject json = reader.readObject();
          SpectralDBEntry entry = getDBEntry(json);
          if (entry != null) {
            list.add(entry);
            // progress
            if (list.size() % 1000 == 0) {
              logger.info("Imported " + list.size() + " library entries");
              // start task for every 1000 entries
              SpectralMatchTask task = new SpectralMatchTask(parameters, tasks.size() * 1000 + 1,
                  list, spectraPlot, scan);
              MZmineCore.getTaskController().addTask(task);
              tasks.add(task);
              list = new ArrayList<>();
            }
          } else
            error++;
        } catch (Exception ex) {
          error++;
          logger.log(Level.WARNING, "Error for entry", ex);
        } finally {
          if (reader != null)
            reader.close();
        }
        // to many errors? wrong data format?
        if (error > 5 && list.isEmpty() && tasks.isEmpty()) {
          logger.log(Level.WARNING, "This file was no MONA spectral json library");
          return tasks;
        }
      }
    }
    // start last task
    logger.info((tasks.size() * 1000 + list.size()) + " MONA library entries imported");
    if (!list.isEmpty()) {
      SpectralMatchTask task =
          new SpectralMatchTask(parameters, tasks.size() * 1000 + 1, list, spectraPlot, scan);
      MZmineCore.getTaskController().addTask(task);
      tasks.add(task);
    }
    return tasks;
  }

  public SpectralDBEntry getDBEntry(JsonObject main) {
    // extract dps
    DataPoint[] dps = getDataPoints(main);
    if (dps == null || dps.length == 0)
      return null;
    // metadata
    Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    extractAllFields(main, map);
    return new SpectralDBEntry(map, dps);
  }

  private void extractAllFields(JsonObject main, Map<DBEntryField, Object> map) {
    for (DBEntryField f : DBEntryField.values()) {
      Object value = null;
      JsonValue j = null;

      switch (f) {
        case INCHI:
          value = readCompound(main, "inchi");
          if (value == null) {
            value = readCompoundMetaData(main, "InChI");
          }
          break;
        case INCHIKEY:
          value = readCompound(main, "inchiKey");
          if (value == null) {
            value = readCompoundMetaData(main, "InChIKey");
          }
          break;
        case ACQUISITION:
          break;
        case MONA_ID:
          value = readMetaData(main, "accession");
          break;
        case CAS:
          // TODO check real id (cas CAS ?)
          value = readCompoundMetaData(main, "cas");
          break;
        case CHARGE:
          break;
        case COLLISION_ENERGY:
          value = readMetaData(main, "collision energy");
          break;
        case COMMENT:
          break;
        case DATA_COLLECTOR:
          value = readMetaData(main, "author");
          break;
        case INSTRUMENT:
          value = readMetaData(main, "instrument");
          break;
        case INSTRUMENT_TYPE:
          value = readMetaData(main, "instrument type");
          break;
        case MS_LEVEL:
          value = readMetaData(main, "ms level");
          break;
        case RESOLUTION:
          value = readMetaData(main, "resolution");
          if (value != null)
            value = value.toString();
          break;
        case IONTYPE:
          value = readMetaData(main, "precursor type");
          break;
        case ION_MODE:
          value = readMetaData(main, "ionization mode");
          break;
        case ION_SOURCE:
          value = readMetaData(main, "ionization");
          break;
        case EXACT_MASS:
          value = readMetaDataDouble(main, "exact mass");
          break;
        case MOLWEIGHT:
          value = readMetaDataDouble(main, "exact mass");
          break;
        case MZ:
          value = readMetaDataDouble(main, "precursor m/z");
          break;
        case NAME:
          // can have multiple names
          JsonArray names = main.getJsonArray(COMPOUND).getJsonObject(0).getJsonArray("names");
          value = names.stream().map(v -> v.asJsonObject()).map(v -> v.getString("name", null))
              .filter(Objects::nonNull).collect(Collectors.joining(", "));
          break;
        case NUM_PEAKS:
          break;
        case PRINZIPLE_INVESTIGATOR:
          value = readMetaData(main, "author");
          break;
        case CHEMSPIDER:
          j = readCompoundMetaDataJson(main, "chemspider");
          if (j != null) {
            if (j.getValueType().equals(ValueType.STRING))
              value = ((JsonString) j).getString();
            if (j.getValueType().equals(ValueType.NUMBER))
              value = ((JsonNumber) j).intValue();
          }
          break;
        case PUBCHEM:
          j = readCompoundMetaDataJson(main, "pubchem cid");
          if (j != null) {
            if (j.getValueType().equals(ValueType.STRING))
              value = ((JsonString) j).getString();
            if (j.getValueType().equals(ValueType.NUMBER))
              value = ((JsonNumber) j).intValue();
          }
          break;
        case FORMULA:
          value = readCompoundMetaData(main, "molecular formula");
          break;
        case PUBMED:
          break;
        case RT:
          Object tmp = readMetaData(main, "retention time");
          if (tmp != null) {
            if (tmp instanceof Number)
              value = ((Number) tmp).doubleValue();
            else {
              try {
                String v = (String) tmp;
                v = v.replaceAll(" ", "");
                // to minutes
                if (v.endsWith("sec")) {
                  v = v.substring(0, v.length() - 3);
                  value = Double.parseDouble(v) / 60d;
                } else {
                  value = Double.parseDouble(v);
                }
              } catch (Exception ex) {
              }
            }
          }
          break;
        case SMILES:
          value = readCompoundMetaData(main, "SMILES");
          break;
        case SOFTWARE:
          break;
        case SYNONYM:
          break;
        default:
          break;
      }

      if (value != null && value.equals("N/A"))
        value = null;
      // add value
      if (value != null) {
        // add
        map.put(f, value);
      }
    }
  }

  /**
   * read from META_DATA array
   * 
   * @param main
   * @param id
   * @return String or Number or null
   */
  private Object readMetaData(JsonObject main, String id) {
    JsonValue j = main.getJsonArray(META_DATA).stream().map(v -> v.asJsonObject())
        .filter(v -> v.getString("name").equals(id)).map(v -> v.get("value")).findFirst()
        .orElse(null);

    if (j != null) {
      if (j.getValueType().equals(ValueType.STRING))
        return ((JsonString) j).getString();
      if (j.getValueType().equals(ValueType.NUMBER))
        return ((JsonNumber) j).numberValue();
    }
    return null;
  }

  private Double readMetaDataDouble(JsonObject main, String id) {
    return main.getJsonArray(META_DATA).stream().map(v -> v.asJsonObject())
        .filter(v -> v.getString("name").equals(id))
        .map(v -> v.getJsonNumber("value").doubleValue()).findFirst().orElse(null);
  }

  private JsonValue readCompoundMetaDataJson(JsonObject main, String id) {
    return main.getJsonArray(COMPOUND).getJsonObject(0).getJsonArray(META_DATA).stream()
        .map(v -> v.asJsonObject()).filter(v -> v.getString("name").equals(id))
        .map(v -> v.get("value")).findFirst().orElse(null);
  }

  /**
   * read from COMPOUND...META_DATA array
   * 
   * @param main
   * @param id
   * @return
   */
  private String readCompoundMetaData(JsonObject main, String id) {
    return main.getJsonArray(COMPOUND).getJsonObject(0).getJsonArray(META_DATA).stream()
        .map(v -> v.asJsonObject()).filter(v -> v.getString("name").equals(id))
        .map(v -> v.getString("value")).findFirst().orElse(null);
  }

  /**
   * Read from COMPOUND object
   * 
   * @param main
   * @param id
   * @return
   */
  private String readCompound(JsonObject main, String id) {
    return main.getJsonArray(COMPOUND).getJsonObject(0).getString(id, null);
  }


  public DataPoint[] getDataPoints(JsonObject main) {
    String spec = main.getString("spectrum");
    if (spec == null)
      return null;
    String[] data = spec.split(" ");
    DataPoint[] dps = new DataPoint[data.length];
    for (int i = 0; i < dps.length; i++) {
      String[] dp = data[i].split(":");
      double mz = Double.parseDouble(dp[0]);
      double intensity = Double.parseDouble(dp[1]);
      dps[i] = new SimpleDataPoint(mz, intensity);
    }
    return dps;
  }

}
