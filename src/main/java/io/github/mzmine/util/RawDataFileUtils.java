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

package io.github.mzmine.util;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_waters_raw.WatersRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Raw data file related utilities
 */
public class RawDataFileUtils {

  private static final Logger logger = Logger.getLogger(RawDataFileUtils.class.getName());

  public static void createRawDataImportTasks(MZmineProject project, List<Task> taskList,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, File... fileNames) throws IOException {

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
    for (File fileName : fileNames) {

      if ((!fileName.exists()) || (!fileName.canRead())) {
        logger.warning("Cannot read file " + fileName);
        continue;
      }

      final RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(fileName);
      RawDataFile newMZmineFile;

      Task newTask = null;
      switch (fileType) {
        case ICPMSMS_CSV:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new IcpMsCVSImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
          break;
        case MZDATA:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new MzDataImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
          break;
        case MZML:
          newTask = new MSDKmzMLImportTask(project, fileName, module, parameters,moduleCallDate, storage);
          break;
        case IMZML:
          newMZmineFile = MZmineCore
              .createNewImagingFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new ImzMLImportTask(project, fileName, (ImagingRawDataFile) newMZmineFile,
              module, parameters, moduleCallDate);
          break;
        case MZXML:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new MzXMLImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
          break;
        case NETCDF:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new NetCDFImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
          break;
        case THERMO_RAW:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new ThermoRawImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
        case WATERS_RAW:
          newMZmineFile = MZmineCore
              .createNewFile(fileName.getName(), fileName.getAbsolutePath(), storage);
          newTask = new WatersRawImportTask(project, fileName, newMZmineFile, module, parameters, moduleCallDate);
          break;
        case MZML_ZIP:
        case MZML_GZIP:
          newTask = new ZipImportTask(project, fileName, module, parameters, moduleCallDate, storage);
          break;
        case BRUKER_TDF:
          newMZmineFile = MZmineCore
              .createNewIMSFile(fileName.getName(), fileName.getAbsolutePath(),
                  MemoryMapStorage.forRawDataFile());
          newTask = new TDFImportTask(project, fileName, (IMSRawDataFile) newMZmineFile, module,
              parameters, moduleCallDate);
          break;
        case MZML_IMS:
          newTask = new MSDKmzMLImportTask(project, fileName, module, parameters, moduleCallDate, storage);
          break;
        default:
          break;
      }
      if (newTask != null) {
        taskList.add(newTask);
      }
    }
  }


  /**
   * Checks the given file names if they have a common prefix and if yes, asks the user whether the
   * user wants to remove the prefix or a part of it. If the user says yes, the method returns the
   * prefix to be removed, otherwise returns null. Only works in GUI mode and when called on the
   * JavaFX thread, otherwise returns null.
   * <p>
   * Assumes that fileNames doesn't contain null entries.
   */
  public static @Nullable String askToRemoveCommonPrefix(@NotNull File fileNames[]) {

    if(true) {
      // currently this will break project load/save, because the files are renamed before they
      // exist. So let's just deactivate it for now.
      return null;
    }

    // If we're running in batch mode or not on the JavaFX thread, give up
    if ((MZmineCore.getDesktop().getMainWindow() == null) || (!Platform.isFxApplicationThread())) {
      return null;
    }

    // We need at least 2 files to have a common prefix
    if (fileNames.length < 2) {
      return null;
    }

    String commonPrefix = null;
    final String firstName = fileNames[0].getName();
    outerloop:
    for (int x = 0; x < firstName.length(); x++) {
      for (int i = 0; i < fileNames.length; i++) {
        if (!firstName.substring(0, x).equals(fileNames[i].getName().substring(0, x))) {
          commonPrefix = firstName.substring(0, x - 1);
          break outerloop;
        }
      }
    }
    // If we didn't find a common prefix, leave here
    if (Strings.isNullOrEmpty(commonPrefix)) {
      return null;
    }

    // Show a dialog to allow user to remove common prefix
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Common prefix");
    dialog.setContentText(
        "The files you have chosen have a common prefix. Would you like to remove some or all of this prefix to shorten the names?");

    TextField textField = new TextField();
    textField.setText(commonPrefix);

    VBox panel = new VBox(10.0);
    panel.getChildren().add(new Label("The files you have chosen have a common prefix."));
    panel.getChildren().add(
        new Label("Would you like to remove some or all of this prefix to shorten the names?"));
    panel.getChildren().add(new Label(" "));
    panel.getChildren().add(new Label("Prefix to remove:"));
    panel.getChildren().add(textField);

    ButtonType removeButtonType = new ButtonType("Remove", ButtonData.YES);
    ButtonType notRemoveButtonType = new ButtonType("Do not remove", ButtonData.NO);
    dialog.getDialogPane().getButtonTypes()
        .addAll(removeButtonType, notRemoveButtonType, ButtonType.CANCEL);
    dialog.getDialogPane().setContent(panel);
    Optional<ButtonType> result = dialog.showAndWait();

    if (!result.isPresent()) {
      return null;
    }

    // Cancel import if user clicked cancel
    if (result.get() == ButtonType.CANCEL) {
      return null;
    }

    // Only remove if user selected to do so
    if (result.get() == removeButtonType) {
      commonPrefix = textField.getText();
    }

    return null;
  }

  public static @NotNull Range<Float> findTotalRTRange(RawDataFile dataFiles[], int msLevel) {
    Range<Float> rtRange = null;
    for (RawDataFile file : dataFiles) {
      Range<Float> dfRange = file.getDataRTRange(msLevel);
      if (dfRange == null) {
        continue;
      }
      if (rtRange == null) {
        rtRange = dfRange;
      } else {
        rtRange = rtRange.span(dfRange);
      }
    }
    if (rtRange == null) {
      rtRange = Range.singleton(0.0f);
    }
    return rtRange;
  }

  public static @NotNull Range<Double> findTotalMZRange(RawDataFile dataFiles[], int msLevel) {
    Range<Double> mzRange = null;
    for (RawDataFile file : dataFiles) {
      Range<Double> dfRange = file.getDataMZRange(msLevel);
      if (dfRange == null) {
        continue;
      }
      if (mzRange == null) {
        mzRange = dfRange;
      } else {
        mzRange = mzRange.span(dfRange);
      }
    }
    if (mzRange == null) {
      mzRange = Range.singleton(0.0);
    }
    return mzRange;
  }

  /**
   * Returns true if the given data file has mass lists for all MS1 scans
   */
  public static boolean hasMassLists(RawDataFile dataFile) {
    List<Scan> scans = dataFile.getScanNumbers(1);
    for (Scan scan : scans) {
      if (scan.getMassList() == null) {
        return false;
      }
    }
    return true;
  }

  public static Scan getClosestScanNumber(RawDataFile dataFile, double rt) {

    ObservableList<Scan> scanNums = dataFile.getScans();
    if (scanNums.size() == 0) {
      return null;
    }
    int best = 0;
    double bestRt = scanNums.get(0).getRetentionTime();

    for (int i = 1; i < scanNums.size(); i++) {
      double thisRt = scanNums.get(i).getRetentionTime();
      if (Math.abs(bestRt - rt) > Math.abs(thisRt - rt)) {
        best = i;
        bestRt = thisRt;
      }
    }
    return scanNums.get(best);
  }
}
