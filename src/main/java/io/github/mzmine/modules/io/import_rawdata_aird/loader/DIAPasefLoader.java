package io.github.mzmine.modules.io.import_rawdata_aird.loader;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.modules.io.import_rawdata_aird.AirdImportTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.BlockIndex;
import net.csibio.aird.bean.CV;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.bean.common.MobilityPoint;
import net.csibio.aird.bean.common.Spectrum;
import net.csibio.aird.constant.PSI;
import net.csibio.aird.parser.DIAPasefParser;

public class DIAPasefLoader {

  public static void load(AirdImportTask task, DIAPasefParser parser) throws IOException {
    AirdInfo airdInfo = parser.getAirdInfo();
    List<BlockIndex> indexList = airdInfo.getIndexList();
    Long totalCount = airdInfo.getTotalCount();
    double[] mobiDict = parser.getMobiDict();
    HashMap<Double, Integer> dictIndexMap = new HashMap<>();
    for (int i = 0; i < mobiDict.length; i++) {
      dictIndexMap.put(mobiDict[i], i);
    }
//    for (int i = 0; i < totalCount; i++) {
//      Spectrum spectrum = parser.getSpectrum(i);
//      List<BuildingMobilityScan> spectra = buildSpectraForTIMSFrame(spectrum, dictIndexMap);
//      SimpleFrame frame = buildSimpleFrame()
//    }
    HashMap<Double, List<Spectrum>> rtMap = new HashMap<>();
    for (BlockIndex index : indexList) {
      TreeMap<Double, Spectrum> map = parser.getSpectra(index);
      List<Double> rtList = index.getRts();
      for (int i = 0; i < rtList.size(); i++) {
        double rt = rtList.get(i);
        Spectrum spectrum = map.get(rt);
        if (!rtMap.containsKey(rt)) {
          rtMap.put(rt, new ArrayList<>());
        }
        rtMap.get(rt).add(spectrum);
      }
    }
    int iter = 1;
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
        List<Spectrum> spectra = rtMap.get(rt);
        if (spectra == null) {
          continue;
        }
        rtMap.remove(rt);
        List<MobilityPoint> points = merge(spectra);
        SimpleFrame frame = buildSimpleFrame(task, points, index.getCvList().get(i),
            rangeList != null ? rangeList.get(0) : null, iter, rt / 60, index.getLevel(),
            polarity == null ? index.getPolarities().get(i) : polarity,
            index.getFilterStrings() != null ? index.getFilterStrings().get(i) : "",
            msType == null ? (index.getMsTypes().get(i)) : msType,
            activator == null ? index.getActivators().get(i) : activator,
            energy == null ? index.getEnergies().get(i) : energy,
            index.getInjectionTimes() != null ? index.getInjectionTimes().get(i) : null, null);
        List<BuildingMobilityScan> subSpectra = buildSpectraForTIMSFrame(frame, points,
            dictIndexMap, mobiDict);
        frame.setMobilityScans(subSpectra, false);
        iter++;
        task.parsedScans++;
        task.newMZmineFile.addScan(frame);
      }
    }
  }

  private static SimpleFrame buildSimpleFrame(AirdImportTask task, List<MobilityPoint> points,
      List<CV> cvList, WindowRange windowRange, Integer num, double rt, int msLevel,
      String polarity, String filterString, String massSpectrum, String activator, Float energy,
      Float injectionTime, Scan parentScan) {
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

    double[] mzs = new double[points.size()];
    double[] ints = new double[points.size()];
    for (int i = 0; i < points.size(); i++) {
      mzs[i] = points.get(i).mz();
      ints[i] = points.get(i).intensity();
    }
    SimpleFrame frame = new SimpleFrame(task.newMZmineFile, num, msLevel, (float) rt, mzs, ints,
        msType, polarityType, filterString, mzRange, MobilityType.TIMS, null, (float) rt);

    return frame;
  }

  private static List<BuildingMobilityScan> buildSpectraForTIMSFrame(SimpleFrame frame,
      List<MobilityPoint> points, HashMap<Double, Integer> dictMap, double[] dictArray) {
    List<BuildingMobilityScan> spectra = new ArrayList<>();
    TreeMap<Double, List<Integer>> mobiMap = new TreeMap<>();
    for (int i = 0; i < points.size(); i++) {
      double mobi = points.get(i).mobility();
      if (!mobiMap.containsKey(mobi)) {
        mobiMap.put(mobi, new ArrayList<>());
      }
      mobiMap.get(mobi).add(i);
    }

    mobiMap.forEach((mobi, indexList) -> {
      double[] mzsTemp = new double[indexList.size()];
      double[] intsTemp = new double[indexList.size()];
      for (int i = 0; i < indexList.size(); i++) {
        mzsTemp[i] = points.get(indexList.get(i)).mz();
        intsTemp[i] = points.get(indexList.get(i)).intensity();
      }
      BuildingMobilityScan scan = new BuildingMobilityScan(dictMap.get(mobi), mzsTemp, intsTemp);
      spectra.add(scan);
    });
    Set<Integer> scanNumberSet = spectra.stream().map(BuildingMobilityScan::getMobilityScanNumber)
        .collect(Collectors.toSet());
    int maxScanNumber = spectra.get(0).getMobilityScanNumber();
    double[] mobilityArray = new double[maxScanNumber + 1];
    for (int i = 0; i <= maxScanNumber; i++) {
      if (!scanNumberSet.contains(i)) {
        spectra.add(new BuildingMobilityScan(i, new double[]{}, new double[]{}));
      }
      mobilityArray[i] = dictArray[i];
    }
    frame.setMobilities(mobilityArray);
    spectra.sort(Comparator.comparing(BuildingMobilityScan::getMobilityScanNumber));
    return spectra;
  }

  public static List<MobilityPoint> merge(List<Spectrum> spectra) {
    int total = 0;
    for (Spectrum spectrum : spectra) {
      total += spectrum.getMzs().length;
    }
    List<MobilityPoint> points = new ArrayList<>(total);
    for (Spectrum spectrum : spectra) {
      for (int i = 0; i < spectrum.getMzs().length; i++) {
        points.add(
            new MobilityPoint(spectrum.getMzs()[i], spectrum.getInts()[i], spectrum.getMobilities()[i]));
      }
    }
    points.sort(Comparator.comparing(MobilityPoint::mz));
    return points;
  }
}
