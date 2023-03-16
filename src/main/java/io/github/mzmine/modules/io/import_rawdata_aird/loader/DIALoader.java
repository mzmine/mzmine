package io.github.mzmine.modules.io.import_rawdata_aird.loader;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.io.import_rawdata_aird.AirdImportTask;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.BlockIndex;
import net.csibio.aird.bean.CV;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.bean.common.Spectrum;
import net.csibio.aird.constant.PSI;
import net.csibio.aird.enums.MsLevel;
import net.csibio.aird.parser.DIAParser;

public class DIALoader {

  public static void load(AirdImportTask task, DIAParser parser) throws IOException {
    AirdInfo airdInfo = parser.getAirdInfo();
    List<BlockIndex> indexList = airdInfo.getIndexList();
    for (BlockIndex index : indexList) {
      TreeMap<Double, Spectrum> map = parser.getSpectra(index);
      List<Integer> numList = index.getNums();
      List<Double> rtList = index.getRts();
      List<WindowRange> rangeList = index.getRangeList();
      String polarity = airdInfo.getPolarity();
      String msType = airdInfo.getMsType();
      String activator = airdInfo.getActivator();
      Float energy = airdInfo.getEnergy();
      for (int i = 0; i < rtList.size(); i++) {
        double rt = rtList.get(i);
        SimpleScan scan = buildSimpleScan(task, map.get(rt), index.getCvList().get(i),
            rangeList != null ? rangeList.get(0) : null, numList.get(i), rt, index.getLevel(),
            polarity == null ? index.getPolarities().get(i) : polarity,
            index.getFilterStrings() != null ? index.getFilterStrings().get(i) : "",
            msType == null ? (index.getMsTypes().get(i)) : msType,
            activator == null ? index.getActivators().get(i) : activator,
            energy == null ? index.getEnergies().get(i) : energy, index.getInjectionTimes().get(i),
            null);
        task.parsedScans++;
        task.newMZmineFile.addScan(scan);
      }
    }
  }

  private static SimpleScan buildSimpleScan(AirdImportTask task, Spectrum spectrum, List<CV> cvList,
      WindowRange windowRange, Integer num, double rt, int msLevel, String polarity,
      String filterString, String massSpectrum, String activator, Float energy, Float injectionTime,
      Scan parentScan) {
    MassSpectrumType msType = null;
    PolarityType polarityType = null;
    Double lowestMz = null;
    Double highestMz = null;

    if (polarity.equals(PolarityType.POSITIVE.name())) {
      polarityType = PolarityType.POSITIVE;
    }
    if (polarity.equals(PolarityType.NEGATIVE.name())) {
      polarityType = PolarityType.NEGATIVE;
    }
    if (massSpectrum.equals(MassSpectrumType.PROFILE.name())) {
      msType = MassSpectrumType.PROFILE;
    }
    if (massSpectrum.equals(MassSpectrumType.CENTROIDED.name())) {
      msType = MassSpectrumType.CENTROIDED;
    }

    if (cvList != null && cvList.size() > 0) {
      for (CV cv : cvList) {
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
      msMsInfo = BaseLoader.buildMsMsInfo(activator, energy, windowRange, parentScan);
    }

    SimpleScan msScan = new SimpleScan(task.newMZmineFile, num + 1, msLevel, (float) rt, msMsInfo,
        spectrum.getMzs(), spectrum.getInts(), msType, polarityType, filterString, mzRange,
        injectionTime);

    return msScan;

  }
}
