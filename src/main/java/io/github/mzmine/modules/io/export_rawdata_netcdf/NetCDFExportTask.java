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

package io.github.mzmine.modules.io.export_rawdata_netcdf;

import java.io.File;
import java.util.logging.Logger;

import io.github.msdk.MSDKMethod;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.MZmineToMSDKRawDataFile;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

public class NetCDFExportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;

  // User parameters
  private File outFilename;

  private MSDKMethod<?> msdkMethod = null;

  /**
   * @param dataFile
   */
  public NetCDFExportTask(RawDataFile dataFile, File outFilename) {
    super(null); // no new data stored -> null
    this.dataFile = dataFile;
    this.outFilename = outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Exporting file " + dataFile + " to " + outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if ((msdkMethod == null) || (msdkMethod.getFinishedPercentage() == null))
      return 0;
    return msdkMethod.getFinishedPercentage().doubleValue();
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    try {

      setStatus(TaskStatus.PROCESSING);

      logger.info("Started export of file " + dataFile + " to " + outFilename);

      MZmineToMSDKRawDataFile msdkDataFile = new MZmineToMSDKRawDataFile(dataFile);

      msdkMethod = new NetCDFFileExportMethod(msdkDataFile, outFilename);

      if (isCanceled())
        return;
      msdkMethod.execute();

      setStatus(TaskStatus.FINISHED);

      logger.info("Finished export of file " + dataFile + " to " + outFilename);

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error in file export: " + e.getMessage());
    }

  }

  @Override
  public void cancel() {
    super.cancel();
    if (msdkMethod != null)
      msdkMethod.cancel();
  }
}
