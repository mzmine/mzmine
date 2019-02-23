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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Exports all files needed for GNPS
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibrarySubmitTask extends AbstractTask {
  private Logger log = Logger.getLogger(this.getClass().getName());
  private Map<LibrarySubmitIonParameters, DataPoint[]> map;
  private int done = 0;

  LibrarySubmitTask(Map<LibrarySubmitIonParameters, DataPoint[]> map) {
    this.map = map;
  }

  @Override
  public double getFinishedPercentage() {
    return (done / map.size());
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (Entry<LibrarySubmitIonParameters, DataPoint[]> e : map.entrySet()) {
      LibrarySubmitIonParameters param = e.getKey();
      DataPoint[] dps = e.getValue();

      String json = generateJSON(param, dps);
      log.info(json);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private String generateJSON(LibrarySubmitIonParameters param, DataPoint[] dps) {
    LibrarySubmitParameters meta = (LibrarySubmitParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    JsonObjectBuilder json = Json.createObjectBuilder();
    json.add("MZ", param.getParameter(LibrarySubmitIonParameters.MZ).getValue());
    json.add("CHARGE", param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue());
    json.add("ADDUCT", param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue());

    // add data points array
    json.add("peaks", genJSONData(dps));

    // add meta data
    for (Parameter<?> p : meta.getParameters()) {
      if (!p.getName().equals("username") && !p.getName().equals("password")
          && !p.getName().equals(LibrarySubmitParameters.LOCALFILE.getName())) {
        String key = p.getName();
        Object value = p.getValue();
        if (value instanceof Double)
          json.add(key, (Double) value);
        else if (value instanceof Integer)
          json.add(key, (Integer) value);
        else
          json.add(key, value.toString());
      }
    }

    return Json.createObjectBuilder().add("spectrum", json.build()).build().toString();
  }

  private JsonArray genJSONData(DataPoint[] dps) {
    JsonArrayBuilder data = Json.createArrayBuilder();
    JsonArrayBuilder signal = Json.createArrayBuilder();
    for (DataPoint dp : dps) {
      signal.add(dp.getMZ());
      signal.add(dp.getIntensity());
      data.add(signal.build());
    }
    return data.build();
  }

  @Override
  public String getTaskDescription() {
    return "Exporting and submitting MS/MS library entries";
  }

}
