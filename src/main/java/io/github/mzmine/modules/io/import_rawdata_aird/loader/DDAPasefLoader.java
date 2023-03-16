package io.github.mzmine.modules.io.import_rawdata_aird.loader;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.io.import_rawdata_aird.AirdImportTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import net.csibio.aird.bean.CV;
import net.csibio.aird.bean.DDAPasefMs;
import net.csibio.aird.constant.PSI;
import net.csibio.aird.enums.MsLevel;
import net.csibio.aird.parser.DDAPasefParser;

public class DDAPasefLoader {

  public static void load(AirdImportTask task, DDAPasefParser parser) throws Exception {
    List<DDAPasefMs> msList = parser.readAllToMemory();
    if (msList.size() == 0) {
      task.setStatus(TaskStatus.ERROR);
      task.setErrorMessage("Parsing Cancelled, No MS1 Scan found.");
      return;
    }
    boolean isMinute = "minute".equals(task.airdInfo.getRtUnit());

    for (int i = 0; i < msList.size(); i++) {
      DDAPasefMs ms1 = msList.get(i);
      SimpleFrame ms1Frame = buildSimpleScan(task, ms1, null, MsLevel.MS1.getCode());
      task.parsedScans++;
      task.newMZmineFile.addScan(ms1Frame);
      if (ms1.getMs2List() != null && ms1.getMs2List().size() != 0) {
        for (int j = 0; j < ms1.getMs2List().size(); j++) {
          DDAPasefMs ms2 = ms1.getMs2List().get(j);
          if (ms2.getCvList() == null || ms2.getCvList().size() == 0) {
            task.setStatus(TaskStatus.ERROR);
            task.setErrorMessage(
                "Please check the 'PSI CV' option when using AirdPro for conversion");
            return;
          }
          SimpleScan ms2Scan = buildSimpleScan(task, ms2, ms1Frame, MsLevel.MS2.getCode());
          task.parsedScans++;
          task.newMZmineFile.addScan(ms2Scan);
        }
      }
    }
  }

  private static SimpleFrame buildSimpleScan(AirdImportTask task, DDAPasefMs ddaMs,
      Frame parentScan, int msLevel) {
    MassSpectrumType massSpectrumType = null;
    PolarityType polarityType = null;
    Double lowestMz = null;
    Double highestMz = null;
    if (ddaMs.getPolarity().equals(PolarityType.POSITIVE.name())) {
      polarityType = PolarityType.POSITIVE;
    }
    if (ddaMs.getPolarity().equals(PolarityType.NEGATIVE.name())) {
      polarityType = PolarityType.NEGATIVE;
    }
    if (ddaMs.getMsType().equals(MassSpectrumType.PROFILE.name())) {
      massSpectrumType = MassSpectrumType.PROFILE;
    }
    if (ddaMs.getMsType().equals(MassSpectrumType.CENTROIDED.name())) {
      massSpectrumType = MassSpectrumType.CENTROIDED;
    }
    if (ddaMs.getCvList() != null && ddaMs.getCvList().size() > 0) {
      for (CV cv : ddaMs.getCvList()) {
        if (cv.getCvid().contains(PSI.cvLowestMz)) {
          lowestMz = Double.parseDouble(cv.getValue());
          continue;
        }
        if (cv.getCvid().contains(PSI.cvHighestMz)) {
          highestMz = Double.parseDouble(cv.getValue());
          continue;
        }
      }
    }

    Range mzRange = null;
    if (lowestMz != null && highestMz != null) {
      mzRange = Range.closed(lowestMz, highestMz);
    }

    DDAMsMsInfoImpl msMsInfo = null;
    if (msLevel == MsLevel.MS2.getCode()) {
      msMsInfo = BaseLoader.buildMsMsInfo(ddaMs.getActivator(), ddaMs.getEnergy(), ddaMs.getRange(),
          parentScan);
    }

    SimpleFrame frame = new SimpleFrame(task.newMZmineFile, ddaMs.getNum() + 1, msLevel,
        ddaMs.getRt().floatValue(), ddaMs.getSpectrum().getMzs(), ddaMs.getSpectrum().getInts(),
        massSpectrumType, polarityType, ddaMs.getFilterString(), mzRange, MobilityType.TIMS, null,
        ddaMs.getInjectionTime());

    return frame;
  }
}
