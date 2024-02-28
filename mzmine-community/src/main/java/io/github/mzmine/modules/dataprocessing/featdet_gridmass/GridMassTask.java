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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder.Chromatogram;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.Format;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GridMassTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;

  // scan counter
  private int totalScans;
  private float procedure = 0;
  private int newPeakID = 0;
  private final ScanSelection scanSelection;
  private Scan[] scans;
  // User parameters
  private final String suffix;
  private final double mzTol;
  private final double intensitySimilarity;
  private final double minimumTimeSpan;
  private final double maximumTimeSpan;
  private final double smoothTimeSpan;
  private final double smoothTimeMZ;
  private final double minimumHeight;
  Datum[][] roi;
  double[] retentionTime;
  private double additionTimeMaxPeaksPerScan; //TODO inspect
  private double smoothMZ; //TODO inspect
  private double rtPerScan;
  private int tolScans;
  private int maxTolScans;
  private int debug;

  private double minMass = 0;
  private double maxMass = 0;

  private ModularFeatureList newFeatureList;

  private final String ignoreTimes;
  private final ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   */
  public GridMassTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(GridMassParameters.scanSelection).getValue();
    this.mzTol = parameters.getParameter(GridMassParameters.mzTolerance).getValue();
    this.minimumTimeSpan = parameters.getParameter(GridMassParameters.timeSpan).getValue()
        .lowerEndpoint();
    this.maximumTimeSpan = parameters.getParameter(GridMassParameters.timeSpan).getValue()
        .upperEndpoint();
    this.minimumHeight = parameters.getParameter(GridMassParameters.minimumHeight).getValue();
    this.suffix = parameters.getParameter(GridMassParameters.suffix).getValue();
    this.intensitySimilarity = parameters.getParameter(GridMassParameters.intensitySimilarity)
        .getValue();
    this.smoothTimeSpan = parameters.getParameter(GridMassParameters.smoothingTimeSpan).getValue();
    this.smoothTimeMZ = parameters.getParameter(GridMassParameters.smoothingTimeMZ).getValue();
    this.debug = ArrayUtils.indexOf(
        parameters.getParameter(GridMassParameters.showDebug).getValue(),
        GridMassParameters.debugLevels);
    this.ignoreTimes = parameters.getParameter(GridMassParameters.ignoreTimes).getValue();
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Detecting chromatograms (RT) in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return procedure;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  public double intensityRatio(double int1, double int2) {
    return Math.min(int1, int2) / Math.max(int1, int2);
  }

  public void setProcedure(int i, int max, float process) {
    float procedureLen = 10.0f;
    procedure = (process + (float) i / (float) max) / procedureLen;
  }

  static int findFirstMass(double mass, Datum[] mzValues) {
    return findFirstMass(mass, mzValues, 0, mzValues.length - 1);
  }

  static int findFirstMass(double mass, Datum[] mzValues, int l, int r) {
    int mid;
    while (l < r) {
      mid = (r + l) / 2;
      if (mzValues[mid].mz > mass) {
        r = mid - 1;
      } else if (mzValues[mid].mz < mass) {
        l = mid + 1;
      } else {
        return mid;
      }
    }
    while (l > 0 && mzValues[l].mz > mass) {
      l--;
    }
    return l;
  }

  int addMaxDatumFromScans(SpotByProbes s, Chromatogram peak) {

    int i, j;
    int adds = 0;
    for (i = s.minScan; i <= s.maxScan; i++) {
      Datum[] di = roi[i];
      if (di != null && di.length > 0) {
        Datum max = new Datum(new SimpleDataPoint(0, -1), 0, new SimpleDataPoint(0, -1));
        int idx = findFirstMass(s.minMZ, di);
        for (j = idx; j < di.length && di[j].mz <= s.maxMZ; j++) {
          Datum d = di[j];
          if (d.spotId == s.spotId) {
            if (d.intensity > max.intensity && d.mz >= s.minMZ && d.intensity > minimumHeight) {
              max = d;
            }
          }
        }
        if (max.intensity > 0) {
          adds++;
          peak.addMzPeak(scans[i], new SimpleDataPoint(max.mzOriginal, max.intensityOriginal));
        }
      }
    }
    return adds;
  }

  void assignSpotIdToDatumsFromScans(SpotByProbes s, int sRadius, double mzRadius) {

    int i, j;
    for (i = s.minScan; i <= s.maxScan; i++) {
      Datum[] di = roi[i];
      if (di != null && di.length > 0) {
        int idx = findFirstMass(s.minMZ - mzRadius, di);
        for (j = idx; j < di.length && di[j].mz <= s.maxMZ + mzRadius; j++) {
          Datum d = di[j];
          if (d.mz >= s.minMZ - mzRadius) {
            if (d.spotId != 0) {
              // Some spot already assigned this to it. Check
              // exactly who is the winner
              Probe p = new Probe(d.mz, d.scan);
              moveProbeToCenter(p, sRadius, mzRadius);
              if (p.mzCenter == s.center.mzCenter && p.scanCenter == s.center.scanCenter) {
                // This datum is actually MINE (s) !!!, this
                // will happen to datums close to spot borders
                // and that compete with other spot
                // System.out.println("Reassigning spot to
                // Id="+s.spotId+" from
                // Spot:"+d.toString());
                s.setSpotIdToDatum(d);
              }
            } else {
              s.setSpotIdToDatum(d);
            }
          }
        }
      }
    }
  }

  void assignSpotIdToDatumsFromSpotId(SpotByProbes s, SpotByProbes s2, double mzRadius) {

    int i, j;
    int oldSpotId = s2.spotId;
    int mxScan = Math.max(s.maxScan, s2.maxScan);
    double minMZ = Math.min(s.minMZ, s2.minMZ);
    double maxMZ = Math.max(s.maxMZ, s2.maxMZ);
    for (i = Math.min(s.minScan, s2.minScan); i <= mxScan; i++) {
      Datum[] di = roi[i];
      if (di != null && di.length > 0) {
        int idx = findFirstMass(minMZ - mzRadius, di);
        for (j = idx; j < di.length && di[j].mz <= maxMZ + mzRadius; j++) {
          Datum d = di[j];
          if (d.spotId == oldSpotId) {
            s.setSpotIdToDatum(d);
          }
        }
      }
    }
  }

  void moveProbeToCenter(Probe p, int sRadius, double mzRadius) {

    int i, j, k;
    double maxMZ, minMZ;
    boolean move = true;
    Datum max = new Datum(new SimpleDataPoint(0, -1), 0, new SimpleDataPoint(0, -1));
    while (move) {
      k = Math.min(totalScans - 1, p.scanCenter + sRadius);
      for (i = Math.max(p.scanCenter - sRadius, 0); i <= k; i++) {
        Datum[] di = roi[i];
        if (di != null && di.length > 0) {
          minMZ = p.mzCenter - mzRadius;
          int idx = findFirstMass(minMZ, di);
          maxMZ = p.mzCenter + mzRadius;
          for (j = idx; j < di.length && di[j].mz <= maxMZ; j++) {
            Datum d = di[j];
            if (d.intensity > max.intensity && d.mz >= minMZ) {
              max = d;
            }
          }
        }
      }
      if (max.intensity >= 0 && (max.mz != p.mzCenter || max.scan != p.scanCenter)) {
        p.mzCenter = max.mz;
        p.scanCenter = max.scan;
        p.intensityCenter = max.intensity;
        // p.moves++;
      } else {
        move = false;
      }
    }
  }

//  double intensityForMZorScan(ArrayList<DatumExpand> deA, double mz, int scan) {
//    double h = -1;
//    int j;
//    for (j = 0; j < deA.size(); j++) {
//      DatumExpand de = deA.get(j);
//      if ((de.dato.scan == scan || de.dato.mz == mz) && de.dato.intensity > h) {
//        h = de.dato.intensity;
//      }
//    }
//    return h;
//  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started GRIDMASS v1.0 [Apr-09-2014] on " + dataFile);

    scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // Check if we have any scans
    //TODO move to parameter check?
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans match the selected criteria");
      return;
    }

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY; //TODO can this be -1?
    for (Scan s : scans) {
      if (s.getRetentionTime() < prevRT) {
        setStatus(TaskStatus.ERROR);
        final String msg = "Retention time of scan #" + s.getScanNumber()
            + " is smaller then the retention time of the previous scan."
            + " Please make sure you only use scans with increasing retention times."
            + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
        setErrorMessage(msg);
        return;
      }
      prevRT = s.getRetentionTime();
    }

    // Create new feature list
    newFeatureList = new ModularFeatureList(dataFile + " " + suffix, getMemoryMapStorage(), dataFile);
    newFeatureList.setSelectedScans(dataFile, List.of(scans));

    // minimumTimeSpan
    Scan scan = scans[0];
    double minRT = scan.getRetentionTime();
    double maxRT = scan.getRetentionTime();
    retentionTime = new double[totalScans];
    int i;
    for (i = 0; i < totalScans; i++) {
      scan = scans[i];
      double irt = scan.getRetentionTime();
      if (irt < minRT) {
        minRT = irt;
      }
      if (irt > maxRT) {
        maxRT = irt;
      }
      retentionTime[i] = irt;
    }
    rtPerScan = (maxRT - minRT) / i;
    // "tolerable" units in scans
    tolScans = Math.max(2, (int) ((minimumTimeSpan / rtPerScan)));
    maxTolScans = Math.max(2, (int) ((maximumTimeSpan / rtPerScan)));

    // Algorithm to find masses:
    // (1) copy masses:intensity > threshold
    // (2) sort intensities descend
    // (3) Find "spot" for each intensity
    // (3.1) if they have not spot ID assigned
    // (3.1.1) Extend mass in mass and time while > 70% pixels > threshold
    // (3.1.2) If extension > mintime ==> mark all pixels with the spot ID
    // (3.1.3) if extension < mintime ==> mark all pixels with spot ID = -1
    // (4) Group spots within a time-tolerance and mass-tolerance

    logger.info("Getting data points on " + dataFile);

    roi = new Datum[totalScans][];
    ArrayList<Datum> roiAL = new ArrayList<>();
    long passed;
    long nopassed;
    minMass = Double.MAX_VALUE; //TODO Change this value to 0?
    maxMass = 0;
    int maxJ = 0;
    boolean[] scanOk = new boolean[totalScans];
    Arrays.fill(scanOk, true);

    logger.info(
        "Smoothing data points on " + dataFile + " (Time min=" + smoothTimeSpan + "; Time m/z="
            + smoothTimeMZ + ")");
    IndexedDataPoint[][] data = smoothDataPoints( 0);

    logger.info("Determining intensities (mass sum) per scan on " + dataFile);
    for (i = 0; i < totalScans; i++) {
      if (isCanceled()) {
        return;
      }

      IndexedDataPoint[] mzv = data[i];
      double prev = (mzv.length > 0 ? mzv[0].datapoint.getMZ() : 0);
      double massSum = 0;

      for (int j = 0; j < mzv.length; j++) {
        if (mzv[j].datapoint.getIntensity() >= minimumHeight) {
          massSum += mzv[j].datapoint.getMZ() - prev;
        }
        prev = mzv[j].datapoint.getMZ();
        if (mzv[j].datapoint.getMZ() < minMass) {
          minMass = mzv[j].datapoint.getMZ();
        }
        if (mzv[j].datapoint.getMZ() > maxMass) {
          maxMass = mzv[j].datapoint.getMZ();
        }
      }
      double dm = 100.0 / (maxMass - minMass);
      if (i % 30 == 0 && debug > 0) {
        System.out.println();
        System.out.print("t=" + Math.round(retentionTime[i] * 100) / 100.0 + ": (in %) ");
      }
      if (scanOk[i]) {
        if (!scanOk[i]) {
          // Disable neighbouring scans, how many ?
          int j;

          for (j = i; j > 0 && retentionTime[j] + additionTimeMaxPeaksPerScan > retentionTime[i];
              j--) {
            scanOk[j] = false;
          }
          for (j = i;
              j < totalScans && retentionTime[j] - additionTimeMaxPeaksPerScan < retentionTime[i];
              j++) {
            scanOk[j] = false;
          }
        }
        if (debug > 0) {
          System.out.print(((int) (massSum * dm)) + (scanOk[i] ? " " : "*** "));
        }
      } else {
        if (debug > 0) {
          System.out.print(((int) (massSum * dm)) + (scanOk[i] ? " " : "* "));
        }
      }
      setProcedure(i, totalScans, 1);
    }

    if (debug > 0) { //TODO change debugging procedure?
      System.out.println();
    }

    String[] it = ignoreTimes.trim().split(", ?");
    for (int j = 0; j < it.length; j++) {
      String[] itj = it[j].split("-");
      if (itj.length == 2) {
        Double a = Double.parseDouble(itj[0].trim());
        Double b = Double.parseDouble(itj[1].trim());
        for (i = Math.abs(Arrays.binarySearch(retentionTime, a));
            i < totalScans && retentionTime[i] <= b; i++) {
          if (retentionTime[i] >= a) {
            scanOk[i] = false;
          }
        }
      }
    }

    passed = 0;
    nopassed = 0;
    for (i = 0; i < totalScans; i++) {
      if (i % 100 == 0 && isCanceled()) {
        return;
      }
      if (scanOk[i]) {
        scan = scans[i];
        IndexedDataPoint[] mzv = data[i];

        ArrayList<Datum> dal = new ArrayList<>();
        int j;
        for (j = 0; j < mzv.length; j++) {
          if (mzv[j].datapoint.getIntensity() >= minimumHeight) {
            SimpleDataPoint origDP = new SimpleDataPoint(scan.getMzValue(mzv[j].index),
                scan.getIntensityValue(mzv[j].index));
            dal.add(new Datum(mzv[j].datapoint, i, origDP));
            passed++;
          } else {
            nopassed++;
          }
        }
        if (j > maxJ) {
          maxJ = j;
        }
        roi[i] = dal.toArray(new Datum[0]);
        roiAL.addAll(dal);
      }
      setProcedure(i, totalScans, 2);
    }
    logger.info(passed + " intensities >= " + minimumHeight + " of " + (passed + nopassed) + " ("
        + Math.round(passed * 10000.0 / (passed + nopassed)) / 100.0 + "%) on " + dataFile);

    // New "probing" algorithm
    // (1) Generate probes all over chromatograms
    // (2) Move each probe to their closest maximum until it cannot find a
    // new maximum
    // (3) assign spot id to each "center" using all points within region

    // (1) Generate probes all over
    double byMZ = Math.max(mzTol * 2, 1e-6);
    int byScan = Math.max(1, tolScans / 4);
    logger.info(
        "Creating Grid of probes on " + dataFile + " every " + mzFormat.format(byMZ) + " m/z and "
            + byScan + " scans");
    double m;
    int ndata = (int) Math.round(
        (((double) totalScans / (double) byScan) + 1) * ((maxMass - minMass + byMZ) / byMZ));
    Probe[] probes = new Probe[ndata];
    int idata = 0;
    for (i = 0; i < totalScans; i += byScan) {
      if (i % 100 == 0 && isCanceled()) {
        return;
      }
      for (m = minMass - (i % 2) * byMZ / 2; m <= maxMass; m += byMZ) {
        probes[idata++] = new Probe(m, i);
      }
      setProcedure(i, totalScans, 3);
    }

    // (2) Move each probe to their closest center
    double mzR = byMZ / 2;
    int scanR = Math.max(byScan - 1, 2);
    logger.info(
        "Finding local maxima for each probe on " + dataFile + " radius: scans=" + scanR + ", m/z="
            + mzR);
    int okProbes = 0;
    for (i = 0; i < idata; i++) {
      if (i % 100 == 0 && isCanceled()) {
        return;
      }
      moveProbeToCenter(probes[i], scanR, mzR);
      if (probes[i].intensityCenter < minimumHeight) {
        probes[i] = null;
      } else {
        okProbes++;
      }
      setProcedure(i, idata, 4);
    }
    if (okProbes > 0) {
      Probe[] pArr = new Probe[okProbes];
      for (okProbes = i = 0; i < idata; i++) {
        if (probes[i] != null) {
          pArr[okProbes++] = probes[i];
        }
      }
      probes = pArr;
    }
    // (3) Assign spot id to each "center"
    logger.info("Sorting probes " + dataFile);
    Arrays.sort(probes);
    logger.info("Assigning spot id to local maxima on " + dataFile);
    SpotByProbes sbp = new SpotByProbes();
    ArrayList<SpotByProbes> spots = new ArrayList<>();
    double mzA = -1;
    int scanA = -1;
    for (i = 0; i < probes.length; i++) {
      if (probes[i] != null && probes[i].intensityCenter >= minimumHeight) {
        if (probes[i].mzCenter != mzA || probes[i].scanCenter != scanA) {
          if (i % 10 == 0 && isCanceled()) {
            return;
          }
          if (sbp.size() > 0) {
            spots.add(sbp);
            sbp.assignSpotId();
          }
          sbp = new SpotByProbes();
          mzA = probes[i].mzCenter;
          scanA = probes[i].scanCenter;
        }
        sbp.addProbe(probes[i]);
      }
      setProcedure(i, probes.length, 5);
    }
    if (sbp.size() > 0) {
      spots.add(sbp);
      sbp.assignSpotId();
    }
    logger.info("Spots:" + spots.size());

    // Assign specific datums to spots to avoid using datums to several
    // spots
    logger.info("Assigning intensities to local maxima on " + dataFile);
    i = 0;
    for (SpotByProbes sx : spots) {
      if (sx.size() > 0) {
        if (i % 100 == 0 && isCanceled()) {
          return;
        }
        assignSpotIdToDatumsFromScans(sx, scanR, mzR);
      }
      setProcedure(i++, spots.size(), 6);
    }

    // (4) Join Tolerable Centers
    logger.info("Joining tolerable maxima on " + dataFile);
    int criticScans = Math.max(1, tolScans / 2);
    int joins = 0;
    for (i = 0; i < spots.size() - 1; i++) {
      SpotByProbes s1 = spots.get(i);
      if (s1.center != null && s1.size() > 0) {
        if (i % 100 == 0 && isCanceled()) {
          return;
        }

        int j;
        for (j = i; j > 0 && j < spots.size() && spots.get(j - 1).center != null
            && spots.get(j - 1).center.mzCenter + mzTol > s1.center.mzCenter; j--)
          ;
        for (; j < spots.size(); j++) {
          SpotByProbes s2 = spots.get(j);
          if (i != j && s2.center != null) {
            if (s2.center.mzCenter - s1.center.mzCenter > mzTol) {
              break;
            }
            int l = Math.min(Math.abs(s1.minScan - s2.minScan), Math.abs(s1.minScan - s2.maxScan));
            int r = Math.min(Math.abs(s1.maxScan - s2.minScan), Math.abs(s1.maxScan - s2.maxScan));
            int d = Math.min(l, r);
            boolean overlap = !(s2.maxScan < s1.minScan || s2.minScan > s1.maxScan);
            if ((d <= criticScans || overlap) && (
                intensityRatio(s1.center.intensityCenter, s2.center.intensityCenter)
                    > intensitySimilarity)) {
              if (debug > 2) {
                System.out.println(
                    "Joining s1 id " + s1.spotId + "=" + mzFormat.format(s1.center.mzCenter)
                        + " mz [" + mzFormat.format(s1.minMZ) + " ~ " + mzFormat.format(s1.maxMZ)
                        + "] time=" + timeFormat.format(retentionTime[s1.center.scanCenter])
                        + " int=" + s1.center.intensityCenter + " with s2 id " + s2.spotId + "="
                        + mzFormat.format(s2.center.mzCenter) + " mz [" + mzFormat.format(s2.minMZ)
                        + " ~ " + mzFormat.format(s2.maxMZ) + "] time=" + timeFormat.format(
                        retentionTime[s2.center.scanCenter]) + " int=" + s2.center.intensityCenter);
              }
              assignSpotIdToDatumsFromSpotId(s1, s2, mzR);
              s1.addProbesFromSpot(s2, true);
              j = i; // restart
              joins++;
            }
          }
        }
      }
      setProcedure(i, spots.size(), 7);
    }
    logger.info("Joins:" + joins);

    // (5) Remove "Large" spanned masses
    logger.info("Removing long and comparable 'masses' on " + dataFile);
    for (i = 0; i < spots.size() - 1; i++) {
      SpotByProbes s1 = spots.get(i);
      if (s1.center != null && s1.size() > 0) {
        if (i % 100 == 0 && isCanceled()) {
          return;
        }
        int totalScans = s1.maxScan - s1.minScan + 1;
        int lScan = s1.minScan;
        int rScan = s1.maxScan;
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        toRemove.add(i);
        int j;
        for (j = i; j > 0 && j < spots.size() && spots.get(j - 1).center != null
            && spots.get(j - 1).center.mzCenter + mzTol > s1.center.mzCenter; j--)
          ;
        for (; j < spots.size(); j++) {
          SpotByProbes s2 = spots.get(j);
          if (i != j && s2.center != null) {
            if (s2.center.mzCenter - s1.center.mzCenter > mzTol) {
              break;
            }
            if (intensityRatio(s1.center.intensityCenter, s2.center.intensityCenter)
                > intensitySimilarity) {
              int dl = Math.min(Math.abs(lScan - s2.minScan), Math.abs(lScan - s2.maxScan));
              int dr = Math.min(Math.abs(rScan - s2.minScan), Math.abs(rScan - s2.maxScan));
              int md = Math.min(dl, dr);
              if (md <= maxTolScans || !(s2.maxScan < lScan || s2.minScan > rScan)) {
                // tolerable distance or intersection
                totalScans += s2.maxScan - s2.minScan + 1;
                toRemove.add(j);
                lScan = Math.min(lScan, s2.minScan);
                rScan = Math.max(rScan, s2.maxScan);
              }
            }
          }
        }
        if (totalScans * rtPerScan > maximumTimeSpan) {
          if (debug > 2) {
            System.out.println("Removing " + toRemove.size() + " masses around " + mzFormat.format(
                s1.center.mzCenter) + " m/z (" + s1.spotId + "), time " + timeFormat.format(
                retentionTime[s1.center.scanCenter]) + ", intensity " + s1.center.intensityCenter
                + ", Total Scans=" + totalScans + " ("
                + Math.round(totalScans * rtPerScan * 1000.0) / 1000.0 + " min).");
          }
          for (Integer J : toRemove) {
            spots.get(J).clear();
          }
        }
      }
      setProcedure(i, spots.size(), 8);
    }

    // Build peaks from assigned datums
    logger.info("Building peak rows on " + dataFile + " (tolereance scans=" + tolScans + ")");
    i = 0;
    for (SpotByProbes sx : spots) {
      if (sx.size() > 0 && sx.maxScan - sx.minScan + 1 >= tolScans) {
        if (i % 100 == 0 && isCanceled()) {
          return;
        }
        sx.buildMaxDatumFromScans(roi, minimumHeight);
        if (sx.getMaxDatumScans() >= tolScans && (sx.getContigousMaxDatumScans() >= tolScans
            || sx.getContigousToMaxDatumScansRatio() > 0.5)) {
          Chromatogram peak = new Chromatogram(dataFile, scans);
          if (addMaxDatumFromScans(sx, peak) > 0) {
            peak.finishChromatogram();
            if (peak.getArea() > 1e-6) {
              newPeakID++;
              ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, newPeakID);
              newRow.addFeature(dataFile,
                  FeatureConvertors.ChromatogramToModularFeature(newFeatureList, peak));
              newRow.setComment(sx.toString(retentionTime));
              newFeatureList.addRow(newRow);
              if (debug > 0) {
                System.out.println(
                    "Peak added id=" + sx.spotId + " " + mzFormat.format(sx.center.mzCenter)
                        + " mz, time=" + timeFormat.format(retentionTime[sx.center.scanCenter])
                        + ", intensity=" + sx.center.intensityCenter + ", probes=" + sx.size()
                        + ", data scans=" + sx.getMaxDatumScans() + ", cont scans="
                        + sx.getContigousMaxDatumScans() + ", cont ratio="
                        + sx.getContigousToMaxDatumScansRatio() + " area = " + peak.getArea());
              }
              if (debug > 1) {
                // Peak info:
                System.out.println(sx);
                sx.printDebugInfo();
              }
            } else {
              if (debug > 0) {
                System.out.println("Ignored by area ~ 0 id=" + sx.spotId + " " + mzFormat.format(
                    sx.center.mzCenter) + " mz, time=" + timeFormat.format(
                    retentionTime[sx.center.scanCenter]) + ", intensity="
                    + sx.center.intensityCenter + ", probes=" + sx.size() + ", data scans="
                    + sx.getMaxDatumScans() + ", cont scans=" + sx.getContigousMaxDatumScans()
                    + ", cont ratio=" + sx.getContigousToMaxDatumScansRatio() + " area = "
                    + peak.getArea());
              }
            }
          }
        } else {
          if (debug > 0) {
            System.out.println(
                "Ignored by continous criteria: id=" + sx.spotId + " " + mzFormat.format(
                    sx.center.mzCenter) + " mz, time=" + timeFormat.format(
                    retentionTime[sx.center.scanCenter]) + ", intensity="
                    + sx.center.intensityCenter + ", probes=" + sx.size() + ", data scans="
                    + sx.getMaxDatumScans() + ", cont scans=" + sx.getContigousMaxDatumScans()
                    + ", cont ratio=" + sx.getContigousToMaxDatumScansRatio());
          }
        }
      } else {
        if (sx.size() > 0) {
          if (debug > 0) {
            System.out.println(
                "Ignored by time range criteria: id=" + sx.spotId + " " + mzFormat.format(
                    sx.center.mzCenter) + " mz, time=" + timeFormat.format(
                    retentionTime[sx.center.scanCenter]) + ", intensity="
                    + sx.center.intensityCenter + ", probes=" + sx.size() + ", data scans="
                    + sx.getMaxDatumScans() + ", cont scans=" + sx.getContigousMaxDatumScans()
                    + ", cont ratio=" + sx.getContigousToMaxDatumScansRatio());
          }
        }
      }
      setProcedure(i++, spots.size(), 9);
    }
    logger.info("Peaks on " + dataFile + " = " + newFeatureList.getNumberOfRows());

    dataFile.getAppliedMethods().forEach(method -> newFeatureList.getAppliedMethods().add(method));
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(GridMassModule.class, parameters, getModuleCallDate()));
    // Add new peaklist to the project
    project.addFeatureList(newFeatureList);

    // Add quality parameters to peaks
    // QualityParameters.calculateQualityParameters(newPeakList);

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished chromatogram builder (RT) on " + dataFile);

  }

private IndexedDataPoint[][] smoothDataPoints(int scanSpan) {
    List<Scan> scanNumbers = dataFile.getScanNumbers(1); //TODO Can the method potentially work with other MS levels?

    DataPoint[][] mzValues = null; // [relative scan][j value]
    DataPoint[] mzValuesJ;
    Scan[] mzValuesScan = null;
    int[] mzValuesMZidx = null;
    IndexedDataPoint[][] newMZValues;
    IndexedDataPoint[] tmpDP = new IndexedDataPoint[0];
    newMZValues = new IndexedDataPoint[this.totalScans][];
    int i, j, si, sj, ii, k, ssi, ssj, m;
    double timeSmoothingMZtol = Math.max(this.smoothTimeMZ, 1e-6);

    int modts = Math.max(1, this.totalScans / 10);

    for (i = 0; i < this.totalScans; i++) {

      if (i % 100 == 0 && isCanceled()) {
        return null;
      }

      // Smoothing in TIME space
      Scan scan = scanNumbers.get(i);
      double rt = this.retentionTime[i];
      DataPoint[] xDP;
      IndexedDataPoint[] iDP;
      sj = si = i;
      int t = 0;

      if (this.smoothTimeSpan > 0 || scanSpan > 0) {
        if (scan != null) {
          for (si = i; si > 1; si--) {
            if (this.retentionTime[si - 1] < rt - this.smoothTimeSpan / 2) {
              break;
            }
          }
          for (sj = i; sj < this.totalScans - 1; sj++) {
            if (this.retentionTime[sj + 1] >= rt + this.smoothTimeSpan / 2) {
              break;
            }
          }
          ssi = i - (scanSpan - 1) / 2;
          ssj = i + (scanSpan - 1) / 2;
          if (ssi < 0) {
            ssj -= ssi;
            ssi = 0;
          }
          if (ssj >= this.totalScans) {
            ssi -= (ssj - this.totalScans + 1);
            ssj = this.totalScans - 1;
          }
          if (sj - si + 1 < scanSpan) {
            si = ssi;
            sj = ssj;
          }
        }
        if (scan != null && sj > si) {
          // Allocate
          if (mzValues == null || mzValues.length < sj - si + 1) {
            mzValues = new DataPoint[sj - si + 1][];
            mzValuesScan = new Scan[sj - si + 1];
            mzValuesMZidx = new int[sj - si + 1];
          }
          // Load Data Points
          for (j = si; j <= sj; j++) {
            int jsi = j - si;
            if (mzValues[jsi] == null || jsi >= mzValuesScan.length - 1 || !mzValuesScan[jsi
                + 1].equals(scanNumbers.get(j))) {
              Scan xscan = scanNumbers.get(j);
              mzValues[jsi] = ScanUtils.extractDataPoints(xscan); // TODO Change deprecated
              mzValuesScan[jsi] = scanNumbers.get(j);
            } else {
              mzValues[jsi] = mzValues[jsi + 1];
              mzValuesScan[jsi] = mzValuesScan[jsi + 1];
            }
            mzValuesMZidx[jsi] = 0;
          }
          // Estimate Averages
          ii = i - si;
          if (tmpDP.length < mzValues[ii].length) {
            tmpDP = new IndexedDataPoint[mzValues[ii].length * 3 / 2]; //TODO 3/2?
          }
          for (k = 0; k < mzValues[ii].length; k++) {
            DataPoint dp = mzValues[ii][k];
            double mz = dp.getMZ();
            double intensity;
            if (dp.getIntensity() > 0) { // only process those > 0
              double a = 0;
              int c = 0;
              int f;
              for (j = 0; j <= sj - si; j++) {
                for (mzValuesJ = mzValues[j]; mzValuesMZidx[j] < mzValuesJ.length - 1 //TODO inspect
                    && mzValuesJ[mzValuesMZidx[j] + 1].getMZ() < mz - timeSmoothingMZtol;
                    mzValuesMZidx[j]++)
                  ;

                f = mzValuesMZidx[j];

                for (m = mzValuesMZidx[j] + 1;
                    m < mzValuesJ.length && mzValuesJ[m].getMZ() < mz + timeSmoothingMZtol; m++) {
                  if (Math.abs(mzValuesJ[m].getMZ() - mz) < Math.abs(mzValuesJ[f].getMZ() - mz)) {
                    f = m;
                  } else {
                    // must always be closest because they are ordered by mass, so stop the search
                    break;
                  }
                }
                if (f > 0 && f < mzValuesJ.length
                    && Math.abs(mzValuesJ[f].getMZ() - mz) <= timeSmoothingMZtol
                    && mzValuesJ[f].getIntensity() > 0) {
                  a += mzValuesJ[f].getIntensity();
                  c++;
                }
              }
              intensity = c > 0 ? a / c : 0;
              if (intensity >= this.minimumHeight) {
                tmpDP[t++] = new IndexedDataPoint(k, new SimpleDataPoint(mz, intensity));
              }
            }
          }

        }
      } else if (scan != null) {
        xDP = ScanUtils.extractDataPoints(scan);
        if (tmpDP.length < xDP.length) {
          tmpDP = new IndexedDataPoint[xDP.length];
        }
        for (k = 0; k < xDP.length; k++) {
          if (xDP[k].getIntensity() >= this.minimumHeight) {
            tmpDP[t++] = new IndexedDataPoint(k, xDP[k]);
          }
        }
      }
      iDP = new IndexedDataPoint[t];
      for (k = 0; k < t; k++) {
        iDP[k] = tmpDP[k];
      }
      newMZValues[i] = iDP;

      setProcedure(i, this.totalScans, 0);

      if (i % modts == 0) {
        logger.info("Smoothing/Caching " + this.dataFile + "..." + (i / modts) * 10 + "%");
      }

    }

    return newMZValues;
  }
}
