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
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.io.IOException;
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
    File timsdataLib = null;
    String libraryFileName;
    try {
      if (com.sun.jna.Platform.isWindows()) {
        libraryFileName = "timsdata.dll";
      } else if (com.sun.jna.Platform.isMac() || com.sun.jna.Platform.isLinux()) {
        libraryFileName = "libtimstdata.so";
      } else {
        throw new MSDKException(
            "Unknown OS, cannot define file suffix. Please contact the developers");
      }
      timsdataLib = Native
          .extractFromResourcePath("vendorlib/bruker/" + libraryFileName,
              getClass().getClassLoader());
    } catch (IOException | MSDKException e) {
      e.printStackTrace();
      logger.info("Failed to load/extract timsdata.dll/.so");
    }

    if (timsdataLib == null) {
      logger.info("TIMS data library could not be loaded.");
      return ExitCode.ERROR;
    }

    TDFLibrary tdfLib = Native.load(timsdataLib.getAbsolutePath(), TDFLibrary.class);

    logger.info(tdfLib.toString());

    FileChooser dc = new FileChooser();
    File file = dc.showOpenDialog(MZmineCore.getDesktop().getMainWindow());
    if(file != null && file.exists()) {
      MZmineCore.getTaskController().addTask(new TDFMetadataReaderTask(file));
    }
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
}
