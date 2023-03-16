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

package io.github.mzmine.modules.io.import_rawdata_aird;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_aird.loader.DDALoader;
import io.github.mzmine.modules.io.import_rawdata_aird.loader.DDAPasefLoader;
import io.github.mzmine.modules.io.import_rawdata_aird.loader.DIALoader;
import io.github.mzmine.modules.io.import_rawdata_aird.loader.DIAPasefLoader;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.constant.SuffixConst;
import net.csibio.aird.enums.AirdType;
import net.csibio.aird.parser.BaseParser;
import net.csibio.aird.parser.DDAParser;
import net.csibio.aird.parser.DDAPasefParser;
import net.csibio.aird.parser.DIAParser;
import net.csibio.aird.parser.DIAPasefParser;
import net.csibio.aird.util.AirdScanUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirdImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AirdImportTask.class.getName());
  public final MZmineProject project;
  public final ParameterSet parameters;
  public final Class<? extends MZmineModule> module;
  public final File file;
  public final String description;
  public RawDataFile newMZmineFile;
  public AirdInfo airdInfo;
  public int totalScans = 0, parsedScans;

  public AirdImportTask(MZmineProject project, File fileToOpen,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.project = project;

    if (fileToOpen.getName().toLowerCase().endsWith(SuffixConst.AIRD)) {
      this.file = new File(AirdScanUtil.getIndexPathByAirdPath(fileToOpen.getPath()));
    } else {
      this.file = fileToOpen;
    }
//    this.newMZmineFile = newMZmineFile;
    description = "Importing aird data file:" + fileToOpen.getName();
    this.parameters = parameters;
    this.module = module;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    BaseParser parser = null;
    try {
      parser = BaseParser.buildParser(file.getPath());

      airdInfo = parser.getAirdInfo();
      if (airdInfo == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Parsing Cancelled, The aird index file(.json, metadata) not exists or the json file is broken.");
        return;
      }

      if (airdInfo.getMobiInfo() != null) {
        newMZmineFile = new IMSRawDataFileImpl(this.file.getName(), file.getAbsolutePath(),
            storage);
      } else {
        newMZmineFile = new RawDataFileImpl(this.file.getName(), file.getAbsolutePath(), storage);
      }

      totalScans = airdInfo.getTotalCount().intValue();
      switch (AirdType.getType(airdInfo.getType())) {
        case DDA -> DDALoader.load(this, (DDAParser) parser);
        case DIA -> DIALoader.load(this, (DIAParser) parser);
        case DDA_PASEF -> DDAPasefLoader.load(this, (DDAPasefParser) parser);
        case DIA_PASEF -> DIAPasefLoader.load(this, (DIAPasefParser) parser);
        default -> {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Unsupported Aird Type:" + airdInfo.getType());
        }
      }
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Error during aird import of file" + file.getName());
      logger.log(Level.WARNING, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error Parsing Aird File:" + ExceptionUtils.exceptionToString(e));
      return;
    } finally {
      if (parser != null) {
        parser.close();
      }
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found!");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");

    newMZmineFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    project.addFile(newMZmineFile);
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return description;
  }


}
