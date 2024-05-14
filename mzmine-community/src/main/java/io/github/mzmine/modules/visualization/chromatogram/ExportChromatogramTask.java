/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Exports a chromatogram to a CSV file.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ExportChromatogramTask extends AbstractTask {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final File exportFile;
  private final TICDataSet dataSet;

  private int progress;
  private int progressMax;

  /**
   * Create the task.
   *
   * @param data data set to export.
   * @param file file to write to.
   */
  public ExportChromatogramTask(final TICDataSet data, final File file, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    dataSet = data;
    exportFile = file;
    progress = 0;
    progressMax = 0;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting chromatogram for " + dataSet.getDataFile().getName();
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void run() {

    // Update the status of this task
    setStatus(TaskStatus.PROCESSING);

    try {

      // Do the export.
      export();

      // Success.
      logger.info("Exported chromatogram for " + dataSet.getDataFile().getName());
      setStatus(TaskStatus.FINISHED);

    } catch (Throwable t) {

      logger.log(Level.SEVERE, "Chromatogram export error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }
  }

  /**
   * Export the chromatogram.
   *
   * @throws IOException if there are i/o problems.
   */
  public void export() throws IOException {

    // Open the writer.
    final BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile));
    try {

      // Write the header row.
      writer.write("RT,I");
      writer.newLine();

      // Write the data points.
      final int itemCount = dataSet.getItemCount(0);
      progressMax = itemCount;
      for (int i = 0; i < itemCount; i++) {

        // Write (x, y) data point row.
        writer.write(dataSet.getX(0, i) + "," + dataSet.getY(0, i));
        writer.newLine();

        progress = i + 1;
      }
    } finally {

      // Close.
      writer.close();
    }
  }
}
