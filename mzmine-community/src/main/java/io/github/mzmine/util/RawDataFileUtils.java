/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoImportTaskDelegator;
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

      Task newTask = null;
      var scanProcessorConfig = ScanImportProcessorConfig.createDefault();
      switch (fileType) {
        case ICPMSMS_CSV:
          newTask = new IcpMsCVSImportTask(project, fileName, module, parameters, moduleCallDate,
              storage);
          break;
        case MZDATA:
          newTask = new MzDataImportTask(project, fileName, module, parameters, moduleCallDate,
              storage);
          break;
        case MZML, MZML_IMS:
          newTask = new MSDKmzMLImportTask(project, fileName, scanProcessorConfig, module,
              parameters, moduleCallDate, storage);
          break;
        case IMZML:
          newTask = new ImzMLImportTask(project, fileName, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
          break;
        case MZXML:
          newTask = new MzXMLImportTask(project, fileName, scanProcessorConfig,
              module, parameters, moduleCallDate, storage);
          break;
        case NETCDF:
          newTask = new NetCDFImportTask(project, fileName, module, parameters, moduleCallDate,
              storage);
          break;
        case THERMO_RAW:
          newTask = new ThermoImportTaskDelegator(storage, moduleCallDate, fileName,
              scanProcessorConfig, project, parameters, module);
          break;
/*        case WATERS_RAW:
          newMZmineFile = MZmineCore.createNewFile(fileName.getName(), fileName.getAbsolutePath(),
              storage);
          newTask = new WatersRawImportTask(project, fileName, newMZmineFile, module, parameters,
              moduleCallDate);
          break;*/
        case MZML_ZIP:
        case MZML_GZIP:
          newTask = new ZipImportTask(project, fileName, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
          break;
        case BRUKER_TDF:
          newTask = new TDFImportTask(project, fileName, MemoryMapStorage.forRawDataFile(), module,
              parameters, moduleCallDate);
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
  public static @Nullable String askToRemoveCommonPrefix(@NotNull File[] fileNames) {

    if (true) {
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
}
