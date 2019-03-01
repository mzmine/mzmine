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

package net.sf.mzmine.modules.peaklistmethods.identification.localdbsearch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit.LibrarySubmitIonParameters;
import net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit.LibrarySubmitParameters;

public class TestJSON {

  public static void main(String[] args) {
    String p = "D:\\test\\library\\guavatest.json";
    File f = new File(p);

    try {
      Map<PeakIdentity, DataPoint[]> map = new HashMap<>();
      List<String> lines = Files.readLines(f, Charsets.UTF_8);
      for (String l : lines) {
        JsonReader reader = Json.createReader(new StringReader(l));
        JsonObject json = reader.readObject();
        map.put(getPeakIdentity(json), getDataPoints(json));
      }

      System.out.println("done");
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static DataPoint[] getDataPoints(JsonObject main) {
    JsonArray data = main.getJsonArray("peaks");
    DataPoint[] dps = new DataPoint[data.size()];
    for (int i = 0; i < data.size(); i++) {
      double mz = data.getJsonArray(i).getJsonNumber(0).doubleValue();
      double intensity = data.getJsonArray(i).getJsonNumber(1).doubleValue();
      dps[i] = new SimpleDataPoint(mz, intensity);
    }
    return dps;
  }

  public static PeakIdentity getPeakIdentity(JsonObject main) {
    String adduct = main.getString(LibrarySubmitIonParameters.ADDUCT.getName(), "");
    JsonNumber mz = main.getJsonNumber(LibrarySubmitIonParameters.MZ.getName());
    String formula = main.getString(LibrarySubmitParameters.FORMULA.getName(), "");
    String name = main.getString(LibrarySubmitParameters.COMPOUND_NAME.getName(), "");
    String id = name + " (" + adduct + ") " + formula;

    System.out.println(id);
    return new SimplePeakIdentity(id, formula, "local MS/MS database", "", "");
  }

}
