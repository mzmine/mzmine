package io.github.mzmine.modules.io.rawdataimport;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import javafx.stage.DirectoryChooser;

public class RawDataImportFolderParameters extends SimpleParameterSet {

  public static final FileNamesParameter fileNames = new FileNamesParameter();

  public RawDataImportFolderParameters() {
    super(new Parameter[]{fileNames});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Import raw data files");

    File lastFiles[] = getParameter(fileNames).getValue();
    if ((lastFiles != null) && (lastFiles.length > 0)) {
      File currentDir = lastFiles[0].getParentFile();
      if ((currentDir != null) && (currentDir.exists())) {
        directoryChooser.setInitialDirectory(currentDir);
      }
    }

    File selectedFile = directoryChooser.showDialog(null);
    if (selectedFile == null) {
      return ExitCode.CANCEL;
    }
    getParameter(fileNames).setValue((new File[]{selectedFile}));

    return ExitCode.OK;

  }

}
