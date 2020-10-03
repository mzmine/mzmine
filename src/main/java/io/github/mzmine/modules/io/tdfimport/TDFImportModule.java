/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.tdfimport;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import io.github.msdk.MSDKException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.tdfimport.datamodel.TDFLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskControlListener;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TDFImportModule implements MZmineRunnableModule {

  public static final String DESCRIPTION = "Imports Bruker TIMS data files (.tdf)";
  public static final String NAME = "TDF import module";

  public static final Logger logger = Logger.getLogger(TDFImportModule.class.getName());

  @Nonnull
  @Override

  public String getDescription() {
    return DESCRIPTION;
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Please choose the .d directory");
    File dir = chooser.showDialog(MZmineCore.getDesktop().getMainWindow());

    // the user closed the dialog without selecting a file, don't annoy him with a message
    if (dir == null) {
      return ExitCode.CANCEL;
    }

    if (!dir.exists() || !dir.isDirectory()) {
      MZmineCore.getDesktop().displayErrorMessage("Invalid directory.");
      return ExitCode.CANCEL;
    }

    if (!dir.getAbsolutePath().endsWith(".d")) {
      MZmineCore.getDesktop().displayErrorMessage("Invalid directory ending.");
      return ExitCode.CANCEL;
    }

    File[] files = getDataFilesFromDir(dir);
    if(files == null) {
      logger.info(dir.getAbsolutePath() + " does not contain .tdf and .tdf_bin");
      return ExitCode.CANCEL;
    }

    MZmineCore.getTaskController().addTask(new TDFReaderTask(files[0], files[1]));
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Nonnull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return TDFImportParameters.class;
  }

  private File[] getDataFilesFromDir(File dir) {
    File[] files = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if (pathname.getAbsolutePath().endsWith(".tdf") || pathname.getAbsolutePath()
            .endsWith(".tdf_bin")) {
          return true;
        }
        return false;
      }
    });
    if(files.length != 2) {
      return null;
    }

    File tdf = Arrays.stream(files).filter(c -> {
      if(c.getAbsolutePath().endsWith(".tdf")) {
        return true;
      }
      return false;
    }).findAny().get();
    File tdf_bin = Arrays.stream(files).filter(c -> {
      if(c.getAbsolutePath().endsWith(".tdf_bin")) {
        return true;
      }
      return false;
    }).findAny().get();

    return new File[] {tdf, tdf_bin};
  }
}
