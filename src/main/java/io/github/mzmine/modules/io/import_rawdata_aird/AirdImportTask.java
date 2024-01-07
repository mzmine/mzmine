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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.BlockIndex;
import net.csibio.aird.bean.CV;
import net.csibio.aird.bean.DDAMs;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.bean.common.Spectrum;
import net.csibio.aird.constant.PSI;
import net.csibio.aird.constant.SuffixConst;
import net.csibio.aird.enums.AirdType;
import net.csibio.aird.enums.MsLevel;
import net.csibio.aird.parser.BaseParser;
import net.csibio.aird.parser.DDAParser;
import net.csibio.aird.parser.v2.DIAParser;
import net.csibio.aird.util.AirdScanUtil;
import net.csibio.aird.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class AirdImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AirdImportTask.class.getName());
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private AirdInfo airdInfo;
  private String description;
  private int totalScans = 0, parsedScans;

  public AirdImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(newMZmineFile.getMemoryMapStorage(), moduleCallDate); // storage in raw data file
    this.project = project;

    if (fileToOpen.getName().toLowerCase().endsWith(SuffixConst.AIRD)) {
      this.file = new File(AirdScanUtil.getIndexPathByAirdPath(fileToOpen.getPath()));
    } else {
      this.file = fileToOpen;
    }
    this.newMZmineFile = newMZmineFile;
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
      totalScans = airdInfo.getTotalScanCount().intValue();
      switch (AirdType.getType(airdInfo.getType())) {
        case DDA -> loadAsDDA((DDAParser) parser);
        case DIA_SWATH -> loadAsDIA((DIAParser) parser);
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

  private void loadAsDDA(DDAParser parser) throws Exception {
    List<DDAMs> msList = parser.readAllToMemory();
    if (msList.size() == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Parsing Cancelled, No MS1 Scan found.");
      return;
    }
    boolean isMinute = airdInfo.getRtUnit().equals("minute");

    for (int i = 0; i < msList.size(); i++) {
      DDAMs ms1 = msList.get(i);
      if (ms1.getCvList() == null || ms1.getCvList().size() == 0) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Please check the 'PSI CV' option when using AirdPro for conversion");
        return;
      }

      SimpleScan ms1Scan = buildSimpleScan(ms1.getSpectrum(), ms1.getCvList(), null, ms1.getNum(),
          ms1.getRt(), MsLevel.MS1.getCode(), null, isMinute);
      parsedScans++;
      newMZmineFile.addScan(ms1Scan);
      if (ms1.getMs2List() != null && ms1.getMs2List().size() != 0) {
        for (int j = 0; j < ms1.getMs2List().size(); j++) {
          DDAMs ms2 = ms1.getMs2List().get(j);
          if (ms2.getCvList() == null || ms2.getCvList().size() == 0) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Please check the 'PSI CV' option when using AirdPro for conversion");
            return;
          }
          SimpleScan ms2Scan = buildSimpleScan(ms2.getSpectrum(), ms2.getCvList(), ms2.getRange(),
              ms2.getNum(), ms2.getRt(), MsLevel.MS2.getCode(), ms1Scan, isMinute);
          parsedScans++;
          newMZmineFile.addScan(ms2Scan);
        }
      }
    }
  }

  private void loadAsDIA(DIAParser parser) throws IOException {
    AirdInfo airdInfo = parser.getAirdInfo();
    boolean isMinute = airdInfo.getRtUnit().equals("minute");
    List<BlockIndex> indexList = airdInfo.getIndexList();
    for (BlockIndex index : indexList) {
      TreeMap<Float, Spectrum> map = parser.getSpectrums(index);
      List<Integer> numList = index.getNums();
      List<Float> rtList = index.getRts();
      List<WindowRange> rangeList = index.getRangeList();
      for (int i = 0; i < rtList.size(); i++) {
        float rt = rtList.get(i);
        SimpleScan scan = buildSimpleScan(map.get(rt), index.getCvList().get(i),
            rangeList != null ? rangeList.get(0) : null, numList.get(i), rt, index.getLevel(), null,
            isMinute);
        parsedScans++;
        newMZmineFile.addScan(scan);
      }
    }
  }

  private SimpleScan buildSimpleScan(Spectrum spectrum, List<CV> cvList, WindowRange windowRange,
      Integer num, float rt, int msLevel, Scan parentScan, boolean isMinute) {
    MassSpectrumType massSpectrumType = null;
    PolarityType polarityType = null;
    String filterString = null;
    Double lowestMz = null;
    Double highestMz = null;

    for (CV cv : cvList) {
      if (cv.getCvid().contains(PSI.cvPolarityPositive)) {
        polarityType = PolarityType.POSITIVE;
        continue;
      }
      if (cv.getCvid().contains(PSI.cvPolarityNegative)) {
        polarityType = PolarityType.NEGATIVE;
        continue;
      }
      if (cv.getCvid().contains(PSI.cvProfileSpectrum)) {
        massSpectrumType = MassSpectrumType.PROFILE;
        continue;
      }
      if (cv.getCvid().contains(PSI.cvCentroidSpectrum)) {
        massSpectrumType = MassSpectrumType.CENTROIDED;
        continue;
      }
      if (cv.getCvid().contains(PSI.cvScanFilterString)) {
        filterString = cv.getValue();
        continue;
      }
      if (cv.getCvid().contains(PSI.cvLowestMz)) {
        lowestMz = Double.parseDouble(cv.getValue());
        continue;
      }
      if (cv.getCvid().contains(PSI.cvHighestMz)) {
        highestMz = Double.parseDouble(cv.getValue());
        continue;
      }
    }

    Range mzRange = null;
    if (lowestMz != null && highestMz != null) {
      mzRange = Range.closed(lowestMz, highestMz);
    }

    DDAMsMsInfoImpl msMsInfo = null;
    if (msLevel == MsLevel.MS2.getCode()) {
      msMsInfo = buildMsMsInfo(airdInfo, windowRange, parentScan);
    }

    SimpleScan msScan = new SimpleScan(newMZmineFile, num, msLevel, rt * (isMinute ? 60 : 1),
        msMsInfo, spectrum.mzs(), ArrayUtil.fromFloatToDouble(spectrum.ints()), massSpectrumType,
        polarityType, filterString, mzRange);
    return msScan;
  }

  private DDAMsMsInfoImpl buildMsMsInfo(AirdInfo airdInfo, WindowRange range, Scan parentScan) {
    Double precursorMz = range.getMz();
    Integer charge = range.getCharge() == 0 ? null : range.getCharge();
    Float energy = airdInfo.getEnergy() == -1 ? null : airdInfo.getEnergy();
    ActivationMethod method = ActivationMethod.valueOf(airdInfo.getActivator());
    if (precursorMz == null) {
      return null;
    }

    DDAMsMsInfoImpl msMsInfo = new DDAMsMsInfoImpl(precursorMz, charge, energy, null, parentScan,
        MsLevel.MS2.getCode(), method, Range.closed(range.getStart(), range.getEnd()));
    return msMsInfo;
  }
}
