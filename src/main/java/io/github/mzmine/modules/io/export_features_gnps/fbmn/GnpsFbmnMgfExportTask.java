/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsFbmnMgfExportTask extends AbstractTask {
  // Logger.
  private final Logger logger = Logger.getLogger(getClass().getName());

  //
  private final FeatureList[] featureLists;
  private final File fileName;
  private final String plNamePattern = "{}";
  private int currentIndex = 0;
  private final MsMsSpectraMergeParameters mergeParameters;

  // by robin
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // seconds
  private NumberFormat rtsForm = new DecimalFormat("0.###");
  // correlation
  private NumberFormat corrForm = new DecimalFormat("0.0000");

  private FeatureListRowsFilter filter;

  GnpsFbmnMgfExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists();

    this.fileName = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).getValue();
    this.filter = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILTER).getValue();
    if (parameters.getParameter(GnpsFbmnExportAndSubmitParameters.MERGE_PARAMETER).getValue()) {
      mergeParameters = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.MERGE_PARAMETER)
          .getEmbeddedParameters();
    } else {
      mergeParameters = null;
    }
  }

  @Override
  public double getFinishedPercentage() {
    if (featureLists.length == 0)
      return 1;
    else
      return currentIndex / featureLists.length;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      currentIndex++;

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "mgf");

      if (!FileAndPathUtil.createDirectory(curFile.getParentFile())) {
        setErrorMessage("Could not create directories for file " + curFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Open file
      FileWriter writer;
      try {
        writer = new FileWriter(curFile);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      try {
        export(featureList, writer, curFile);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error while writing into file " + curFile + ": " + e.getMessage());
        return;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Close file
      try {
        writer.close();
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not close file " + curFile);
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private int export(FeatureList featureList, FileWriter writer, File curFile) throws IOException {
    final String newLine = System.lineSeparator();

    // count exported
    int count = 0;
    for (FeatureListRow row : featureList.getRows()) {
      // do not export if no MSMS
      if (!filter.filter(row)) {
        continue;
      }

      String rowID = Integer.toString(row.getID());
      double retTimeInSeconds = ((row.getAverageRT() * 60 * 100.0) / 100.);

      // Get the MS/MS scan number
      Feature bestFeature = row.getBestFeature();
      if (bestFeature == null) {
        continue;
      }
      Scan msmsScan = row.getMostIntenseFragmentScan();
      if (msmsScan != null) {
        // MS/MS scan must exist, because msmsScanNumber was > 0

        MassList massList = msmsScan.getMassList();

        if (massList == null) {
          setErrorMessage("MS2 scan has no mass list. Run Mass detection on all scans");
          setStatus(TaskStatus.ERROR);
          return count;
        }

        writer.write("BEGIN IONS" + newLine);

        if (rowID != null)
          writer.write("FEATURE_ID=" + rowID + newLine);

        String mass = mzForm.format(row.getAverageMZ());
        if (mass != null)
          writer.write("PEPMASS=" + mass + newLine);

        if (rowID != null) {
          writer.write("SCANS=" + rowID + newLine);
          writer.write("RTINSECONDS=" + rtsForm.format(retTimeInSeconds) + newLine);
        }

        int msmsCharge = Objects.requireNonNullElse(msmsScan.getPrecursorCharge(), 0);
        String msmsPolarity = msmsScan.getPolarity().asSingleChar();
        if (msmsPolarity.equals("0"))
          msmsPolarity = "";
        if (msmsCharge == 0) {
          msmsCharge = 1;
          msmsPolarity = "";
        }
        writer.write("CHARGE=" + msmsCharge + msmsPolarity + newLine);

        writer.write("MSLEVEL=2" + newLine);

        DataPoint[] dataPoints = massList.getDataPoints();
        if (mergeParameters != null) {
          MsMsSpectraMergeModule merger =
              MZmineCore.getModuleInstance(MsMsSpectraMergeModule.class);
          MergedSpectrum spectrum =
              merger.getBestMergedSpectrum(mergeParameters, row);
          if (spectrum != null) {
            dataPoints = spectrum.data;
            writer.write("MERGED_STATS=");
            writer.write(spectrum.getMergeStatsDescription());
            writer.write(newLine);
          }
        }
        for (DataPoint feature : dataPoints) {
          writer.write(mzForm.format(feature.getMZ()) + " " + intensityForm.format(feature.getIntensity())
              + newLine);
        }
        writer.write("END IONS" + newLine);
        writer.write(newLine);
        count++;
      }
    }

    if (count == 0)
      logger.log(Level.WARNING, "No MS/MS scans exported.");
    else
      logger.info(
          MessageFormat.format("Total of {0} feature rows (MS/MS mass lists) were exported ({1})",
              count, featureList.getName()));

    return count;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting GNPS of feature list(s) " + Arrays.toString(featureLists) + " to MGF file(s)";
  }

}
