package io.github.mzmine.modules.io.tdfimport;

import io.github.mzmine.modules.io.tdfimport.datamodel.TDFLibrary;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;

public class TDFImportTask extends AbstractTask {

  private TDFLibrary tdfLibrary;

  private File tdf;
  private File tdfBin;

  /**
   * Bruker tims format:
   * - Folder
   *  - contains multiple files
   *  - one folder per analysis
   *  - .d extension
   *  - *.tdf - SQLite database; contains metadata
   *  - *.tdf_bin - contains peak data
   *
   *  - *.tdf_bin
   *   - list of frames
   *   - frame:
   *    - set of spectra at one specific time
   *    - single spectrum for "each" mobility
   *    - spectrum:
   *     - intensity vs m/z
   */

  /**
   * @param tdfLibrary
   * @param tdf
   * @param tdfBin
   */
  public TDFImportTask(TDFLibrary tdfLibrary, File tdf, File tdfBin) {
    this.tdfLibrary = tdfLibrary;
    this.tdf = tdf;
    this.tdfBin = tdfBin;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // extract frame IDs
  }
}
