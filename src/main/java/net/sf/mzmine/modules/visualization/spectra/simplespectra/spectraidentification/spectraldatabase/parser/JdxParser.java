/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBEntry;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectralMatchTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;

/**
 * Parser for .jdx DB files for spectra database matching
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class JdxParser implements SpectralDBParser {

  private static Logger logger = Logger.getLogger(NistMspParser.class.getName());

  @Override
  public @Nonnull List<SpectralMatchTask> parse(AbstractTask mainTask, ParameterSet parameters,
      File dataBaseFile, SpectraPlot spectraPlot, Scan scan) throws IOException {
    logger.info("Parsing jdx spectral library " + dataBaseFile.getAbsolutePath());
    List<SpectralMatchTask> tasks = new ArrayList<>();
    List<SpectralDBEntry> list = new ArrayList<>();

    boolean isData = false;
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    // create db
    int sep = -1;
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null;) {
        // main task was canceled?
        if (mainTask.isCanceled()) {
          for (SpectralMatchTask t : tasks)
            t.cancel();
          return new ArrayList<>();
        }

        try {
          // meta data?
          sep = isData ? -1 : l.indexOf("=");
          if (sep != -1) {
            DBEntryField field = DBEntryField.forJdxID(l.substring(0, sep));
            if (field != null) {
              String content = l.substring(sep + 1, l.length());
              if (content.length() > 0) {
                try {
                  Object value = field.convertValue(content);
                  fields.put(field, value);
                } catch (Exception e) {
                  logger.log(Level.WARNING, "Cannot convert value type of " + content + " to "
                      + field.getObjectClass().toString(), e);
                }
              }
            }
          } else {
            // data?
            String[] dataPairs = l.split(" ");
            for (String dataPair : dataPairs) {
              String[] data = dataPair.split(",");
              if (data.length == 2) {
                try {
                  dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                      Double.parseDouble(data[1])));
                  isData = true;
                } catch (Exception e) {
                }
              }
            }
          }
          if (l.contains("END")) {
            // row with END
            // add entry and reset
            SpectralDBEntry entry =
                new SpectralDBEntry(fields, dps.toArray(new DataPoint[dps.size()]));
            fields = new EnumMap<>(fields);
            dps.clear();
            list.add(entry);
            // progress
            if (list.size() % 1000 == 0) {
              // start new task for every 1000 entries
              logger.info("Imported " + list.size() + " library entries");
              SpectralMatchTask task = new SpectralMatchTask(parameters, tasks.size() * 1000 + 1,
                  list, spectraPlot, scan);
              MZmineCore.getTaskController().addTask(task);
              tasks.add(task);
              // new list
              list = new ArrayList<>();
            }
            // reset
            isData = false;
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error for entry", ex);
        }
      }
    }
    if (!list.isEmpty()) {
      // start last task
      SpectralMatchTask task =
          new SpectralMatchTask(parameters, tasks.size() * 1000 + 1, list, spectraPlot, scan);
      MZmineCore.getTaskController().addTask(task);
      tasks.add(task);
    }

    logger.info((tasks.size() * 1000 + list.size()) + " jdx library entries imported");
    return tasks;
  }

}
