/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.masslistmethods.chromatogrambuilder.Chromatogram;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ArrayUtils;

public class GridMassTask extends AbstractTask {

    private HashMap<Integer, DataPoint[]> dpCache = null;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private final RawDataFile dataFile;

    // scan counter
    private int totalScans;
    private float procedure = 0;
    private int newPeakID = 0;
    private ScanSelection scanSelection;
    private Scan[] scans;
    private int scanNumbers[];
    Datum[] roi[];
    double retentiontime[];

    // User parameters
    private String suffix;
    private double mzTol;
    private double intensitySimilarity;
    private double minimumTimeSpan, maximumTimeSpan;
    private double smoothTimeSpan, smoothTimeMZ, smoothMZ;
    private double additionTimeMaxPeaksPerScan;
    private double minimumHeight;
    private double rtPerScan;
    private int tolScans;
    private int maxTolScans;
    private int debug = 0;

    private double minMasa = 0;
    private double maxMasa = 0;

    private SimplePeakList newPeakList;

    private String ignoreTimes = "";

    /**
     * @param dataFile
     * @param parameters
     */
    public GridMassTask(MZmineProject project, RawDataFile dataFile,
	    ParameterSet parameters) {

	this.project = project;
	this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(GridMassParameters.scanSelection)
                .getValue();
	this.mzTol = parameters.getParameter(GridMassParameters.mzTolerance)
		.getValue();
	this.minimumTimeSpan = parameters
		.getParameter(GridMassParameters.timeSpan).getValue()
		.lowerEndpoint();
	this.maximumTimeSpan = parameters
		.getParameter(GridMassParameters.timeSpan).getValue()
		.upperEndpoint();
	this.minimumHeight = parameters.getParameter(
		GridMassParameters.minimumHeight).getValue();
	this.suffix = parameters.getParameter(GridMassParameters.suffix)
		.getValue();
	this.intensitySimilarity = parameters.getParameter(
		GridMassParameters.intensitySimilarity).getValue();
	this.smoothTimeSpan = parameters.getParameter(
		GridMassParameters.smoothingTimeSpan).getValue();
	this.smoothTimeMZ = parameters.getParameter(
		GridMassParameters.smoothingTimeMZ).getValue();
	this.debug = ArrayUtils.indexOf(
		parameters.getParameter(GridMassParameters.showDebug)
			.getValue(), GridMassParameters.debugLevels);
	this.ignoreTimes = parameters.getParameter(
		GridMassParameters.ignoreTimes).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Detecting chromatograms (RT) in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	return procedure;
    }

    public RawDataFile getDataFile() {
	return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
	Format timeFormat = MZmineCore.getConfiguration().getRTFormat();

	setStatus(TaskStatus.PROCESSING);

	logger.info("Started GRIDMASS v1.0 [Apr-09-2014] on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        scanNumbers = scanSelection.getMatchingScanNumbers(dataFile);
        totalScans = scans.length;

        // Check if we have any scans
        if (totalScans == 0) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("No scans match the selected criteria");
            return;
        }

        // Check if the scans are properly ordered by RT
        double prevRT = Double.NEGATIVE_INFINITY;
        for (Scan s : scans) {
            if (s.getRetentionTime() < prevRT) {
                setStatus(TaskStatus.ERROR);
                final String msg = "Retention time of scan #"
                        + s.getScanNumber()
                        + " is smaller then the retention time of the previous scan."
                        + " Please make sure you only use scans with increasing retention times."
                        + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
                setErrorMessage(msg);
                return;
            }
            prevRT = s.getRetentionTime();
        }

	// Create new peak list
	newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

	int j;
	// minimumTimeSpan
	Scan scan = scans[0];
	double minRT = scan.getRetentionTime();
	double maxRT = scan.getRetentionTime();
	retentiontime = new double[totalScans];
	int i;
	for (i = 0; i < totalScans; i++) {
	    scan = scans[i];
	    double irt = scan.getRetentionTime();
	    if (irt < minRT)
		minRT = irt;
	    if (irt > maxRT)
		maxRT = irt;
	    retentiontime[i] = irt;
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
	ArrayList<Datum> roiAL = new ArrayList<Datum>();
	long passed = 0, nopassed = 0;
	minMasa = Double.MAX_VALUE;
	maxMasa = 0;
	int maxJ = 0;
	boolean[] scanOk = new boolean[totalScans];
	Arrays.fill(scanOk, true);

	logger.info("Smoothing data points on " + dataFile + " (Time min="
		+ smoothTimeSpan + "; Time m/z=" + smoothTimeMZ + ")");
	IndexedDataPoint[][] data = smoothDataPoints(dataFile, smoothTimeSpan,
		smoothTimeMZ, 0, smoothMZ, 0, minimumHeight);

	logger.info("Determining intensities (mass sum) per scan on "
		+ dataFile);
	for (i = 0; i < totalScans; i++) {
	    if (isCanceled())
		return;
	    scan = scans[i];
	    IndexedDataPoint mzv[] = data[i]; // scan.getDataPoints();
	    double prev = (mzv.length > 0 ? mzv[0].datapoint.getMZ() : 0);
	    double massSum = 0;
	    for (j = 0; j < mzv.length; j++) {
		if (mzv[j].datapoint.getIntensity() >= minimumHeight)
		    massSum += mzv[j].datapoint.getMZ() - prev;
		prev = mzv[j].datapoint.getMZ();
		if (mzv[j].datapoint.getMZ() < minMasa)
		    minMasa = mzv[j].datapoint.getMZ();
		if (mzv[j].datapoint.getMZ() > maxMasa)
		    maxMasa = mzv[j].datapoint.getMZ();
	    }
	    double dm = 100.0 / (maxMasa - minMasa);
	    if (i % 30 == 0 && debug > 0) {
		System.out.println("");
		System.out.print("t=" + Math.round(retentiontime[i] * 100)
			/ 100.0 + ": (in %) ");
	    }
	    if (scanOk[i]) {
		if (!scanOk[i]) {
		    // Disable neighbouring scans, how many ?
		    for (j = i; j > 0
			    && retentiontime[j] + additionTimeMaxPeaksPerScan > retentiontime[i]; j--) {
			scanOk[j] = false;
		    }
		    for (j = i; j < totalScans
			    && retentiontime[j] - additionTimeMaxPeaksPerScan < retentiontime[i]; j++) {
			scanOk[j] = false;
		    }
		}
		if (debug > 0)
		    System.out.print(((int) (massSum * dm))
			    + (scanOk[i] ? " " : "*** "));
	    } else {
		if (debug > 0)
		    System.out.print(((int) (massSum * dm))
			    + (scanOk[i] ? " " : "* "));
	    }
	    setProcedure(i, totalScans, 1);
	}

	if (debug > 0)
	    System.out.println("");

	String[] it = ignoreTimes.trim().split(", ?");
	for (j = 0; j < it.length; j++) {
	    String itj[] = it[j].split("-");
	    if (itj.length == 2) {
		Double a = Double.parseDouble(itj[0].trim());
		Double b = Double.parseDouble(itj[1].trim());
		for (i = Math.abs(Arrays.binarySearch(retentiontime, a)); i < totalScans
			&& retentiontime[i] <= b; i++) {
		    if (retentiontime[i] >= a) {
			scanOk[i] = false;
		    }
		}
	    }
	}

	passed = 0;
	nopassed = 0;
	for (i = 0; i < totalScans; i++) {
	    if (i % 100 == 0 && isCanceled())
		return;
	    if (scanOk[i]) {
		scan = scans[i];
		IndexedDataPoint mzv[] = data[i];
		DataPoint mzvOriginal[] = scan.getDataPoints();
		ArrayList<Datum> dal = new ArrayList<Datum>();
		for (j = 0; j < mzv.length; j++) {
		    if (mzv[j].datapoint.getIntensity() >= minimumHeight) {
			dal.add(new Datum(mzv[j].datapoint, i,
				mzvOriginal[mzv[j].index]));
			passed++;
		    } else {
			nopassed++;
		    }
		}
		if (j > maxJ)
		    maxJ = j;
		roi[i] = dal.toArray(new Datum[0]);
		roiAL.addAll(dal);
	    }
	    setProcedure(i, totalScans, 2);
	}
	logger.info(passed + " intensities >= " + minimumHeight + " of "
		+ (passed + nopassed) + " ("
		+ Math.round(passed * 10000.0 / (double) (passed + nopassed))
		/ 100.0 + "%) on " + dataFile);

	// New "probing" algorithm
	// (1) Generate probes all over chromatograms
	// (2) Move each probe to their closest maximum until it cannot find a
	// new maximum
	// (3) assign spot id to each "center" using all points within region

	// (1) Generate probes all over
	double byMZ = Math.max(mzTol * 2, 1e-6);
	int byScan = Math.max(1, tolScans / 4);
	logger.info("Creating Grid of probes on " + dataFile + " every "
		+ mzFormat.format(byMZ) + " m/z and " + byScan + " scans");
	double m;
	int ndata = (int) Math
		.round((((double) totalScans / (double) byScan) + 1)
			* ((maxMasa - minMasa + byMZ) / byMZ));
	Probe probes[] = new Probe[ndata];
	int idata = 0;
	for (i = 0; i < totalScans; i += byScan) {
	    if (i % 100 == 0 && isCanceled())
		return;
	    for (m = minMasa - (i % 2) * byMZ / 2; m <= maxMasa; m += byMZ) {
		probes[idata++] = new Probe(m, i);
	    }
	    setProcedure(i, totalScans, 3);
	}

	// (2) Move each probe to their closest center
	double mzR = byMZ / 2;
	int scanR = Math.max(byScan - 1, 2);
	logger.info("Finding local maxima for each probe on " + dataFile
		+ " radius: scans=" + scanR + ", m/z=" + mzR);
	int okProbes = 0;
	for (i = 0; i < idata; i++) {
	    if (i % 100 == 0 && isCanceled())
		return;
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
	    pArr = null;
	}
	// (3) Assign spot id to each "center"
	logger.info("Sorting probes " + dataFile);
	Arrays.sort(probes);
	logger.info("Assigning spot id to local maxima on " + dataFile);
	SpotByProbes sbp = new SpotByProbes();
	ArrayList<SpotByProbes> spots = new ArrayList<SpotByProbes>();
	double mzA = -1;
	int scanA = -1;
	for (i = 0; i < probes.length; i++) {
	    if (probes[i] != null && probes[i].intensityCenter >= minimumHeight) {
		if (probes[i].mzCenter != mzA || probes[i].scanCenter != scanA) {
		    if (i % 10 == 0 && isCanceled())
			return;
		    if (sbp.size() > 0) {
			spots.add(sbp);
			sbp.assignSpotId();
			// System.out.println(sbp.toString());
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
	    // System.out.println(sbp.toString());
	}
	logger.info("Spots:" + spots.size());

	// Assign specific datums to spots to avoid using datums to several
	// spots
	logger.info("Assigning intensities to local maxima on " + dataFile);
	i = 0;
	for (SpotByProbes sx : spots) {
	    if (sx.size() > 0) {
		if (i % 100 == 0 && isCanceled())
		    return;
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
		if (i % 100 == 0 && isCanceled())
		    return;
		for (j = i; j > 0
			&& j < spots.size()
			&& spots.get(j - 1).center != null
			&& spots.get(j - 1).center.mzCenter + mzTol > s1.center.mzCenter; j--)
		    ;
		for (; j < spots.size(); j++) {
		    SpotByProbes s2 = spots.get(j);
		    if (i != j && s2.center != null) {
			if (s2.center.mzCenter - s1.center.mzCenter > mzTol)
			    break;
			int l = Math.min(Math.abs(s1.minScan - s2.minScan),
				Math.abs(s1.minScan - s2.maxScan));
			int r = Math.min(Math.abs(s1.maxScan - s2.minScan),
				Math.abs(s1.maxScan - s2.maxScan));
			int d = Math.min(l, r);
			boolean overlap = !(s2.maxScan < s1.minScan || s2.minScan > s1.maxScan);
			if ((d <= criticScans || overlap)
				&& (intensityRatio(s1.center.intensityCenter,
					s2.center.intensityCenter) > intensitySimilarity)) {
			    if (debug > 2)
				System.out
					.println("Joining s1 id "
						+ s1.spotId
						+ "="
						+ mzFormat
							.format(s1.center.mzCenter)
						+ " mz ["
						+ mzFormat.format(s1.minMZ)
						+ " ~ "
						+ mzFormat.format(s1.maxMZ)
						+ "] time="
						+ timeFormat
							.format(retentiontime[s1.center.scanCenter])
						+ " int="
						+ s1.center.intensityCenter
						+ " with s2 id "
						+ s2.spotId
						+ "="
						+ mzFormat
							.format(s2.center.mzCenter)
						+ " mz ["
						+ mzFormat.format(s2.minMZ)
						+ " ~ "
						+ mzFormat.format(s2.maxMZ)
						+ "] time="
						+ timeFormat
							.format(retentiontime[s2.center.scanCenter])
						+ " int="
						+ s2.center.intensityCenter);
			    assignSpotIdToDatumsFromSpotId(s1, s2, scanR, mzR);
			    s1.addProbesFromSpot(s2, true);
			    j = i; // restart
			    joins++;
			}
			// }
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
		if (i % 100 == 0 && isCanceled())
		    return;
		int totalScans = s1.maxScan - s1.minScan + 1;
		int lScan = s1.minScan;
		int rScan = s1.maxScan;
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		toRemove.add(i);
		for (j = i; j > 0
			&& j < spots.size()
			&& spots.get(j - 1).center != null
			&& spots.get(j - 1).center.mzCenter + mzTol > s1.center.mzCenter; j--)
		    ;
		for (; j < spots.size(); j++) {
		    SpotByProbes s2 = spots.get(j);
		    if (i != j && s2.center != null) {
			if (s2.center.mzCenter - s1.center.mzCenter > mzTol)
			    break;
			if (intensityRatio(s1.center.intensityCenter,
				s2.center.intensityCenter) > intensitySimilarity) {
			    int dl = Math.min(Math.abs(lScan - s2.minScan),
				    Math.abs(lScan - s2.maxScan));
			    int dr = Math.min(Math.abs(rScan - s2.minScan),
				    Math.abs(rScan - s2.maxScan));
			    int md = Math.min(dl, dr);
			    if (md <= maxTolScans
				    || !(s2.maxScan < lScan || s2.minScan > rScan)) {
				// distancia tolerable o intersectan
				totalScans += s2.maxScan - s2.minScan + 1;
				toRemove.add(j);
				lScan = Math.min(lScan, s2.minScan);
				rScan = Math.max(rScan, s2.maxScan);
			    }
			}
		    }
		}
		if (totalScans * rtPerScan > maximumTimeSpan) {
		    if (debug > 2)
			System.out
				.println("Removing "
					+ toRemove.size()
					+ " masses around "
					+ mzFormat.format(s1.center.mzCenter)
					+ " m/z ("
					+ s1.spotId
					+ "), time "
					+ timeFormat
						.format(retentiontime[s1.center.scanCenter])
					+ ", intensity "
					+ s1.center.intensityCenter
					+ ", Total Scans="
					+ totalScans
					+ " ("
					+ Math.round(totalScans * rtPerScan
						* 1000.0) / 1000.0 + " min).");
		    for (Integer J : toRemove) {
			// System.out.println("Removing: "+spots.get(J).spotId);
			spots.get(J).clear();
		    }
		}
	    }
	    setProcedure(i, spots.size(), 8);
	}

	// Build peaks from assigned datums
	logger.info("Building peak rows on " + dataFile + " (tolereance scans="
		+ tolScans + ")");
	i = 0;
	for (SpotByProbes sx : spots) {
	    if (sx.size() > 0 && sx.maxScan - sx.minScan + 1 >= tolScans) {
		if (i % 100 == 0 && isCanceled())
		    return;
		sx.buildMaxDatumFromScans(roi, minimumHeight);
		if (sx.getMaxDatumScans() >= tolScans
			&& (sx.getContigousMaxDatumScans() >= tolScans || sx
				.getContigousToMaxDatumScansRatio() > 0.5)) {
		    Chromatogram peak = new Chromatogram(dataFile, scanNumbers);
		    if (addMaxDatumFromScans(sx, peak) > 0) {
			peak.finishChromatogram();
			if (peak.getArea() > 1e-6) {
			    newPeakID++;
			    SimplePeakListRow newRow = new SimplePeakListRow(
				    newPeakID);
			    newRow.addPeak(dataFile, peak);
			    newRow.setComment(sx.toString(retentiontime));
			    newPeakList.addRow(newRow);
			    if (debug > 0)
				System.out
					.println("Peak added id="
						+ sx.spotId
						+ " "
						+ mzFormat
							.format(sx.center.mzCenter)
						+ " mz, time="
						+ timeFormat
							.format(retentiontime[sx.center.scanCenter])
						+ ", intensity="
						+ sx.center.intensityCenter
						+ ", probes="
						+ sx.size()
						+ ", data scans="
						+ sx.getMaxDatumScans()
						+ ", cont scans="
						+ sx.getContigousMaxDatumScans()
						+ ", cont ratio="
						+ sx.getContigousToMaxDatumScansRatio()
						+ " area = " + peak.getArea());
			    if (debug > 1) {
				// Peak info:
				System.out.println(sx.toString());
				sx.printDebugInfo();
			    }
			} else {
			    if (debug > 0)
				System.out
					.println("Ignored by area ~ 0 id="
						+ sx.spotId
						+ " "
						+ mzFormat
							.format(sx.center.mzCenter)
						+ " mz, time="
						+ timeFormat
							.format(retentiontime[sx.center.scanCenter])
						+ ", intensity="
						+ sx.center.intensityCenter
						+ ", probes="
						+ sx.size()
						+ ", data scans="
						+ sx.getMaxDatumScans()
						+ ", cont scans="
						+ sx.getContigousMaxDatumScans()
						+ ", cont ratio="
						+ sx.getContigousToMaxDatumScansRatio()
						+ " area = " + peak.getArea());
			}
		    }
		} else {
		    if (debug > 0)
			System.out
				.println("Ignored by continous criteria: id="
					+ sx.spotId
					+ " "
					+ mzFormat.format(sx.center.mzCenter)
					+ " mz, time="
					+ timeFormat
						.format(retentiontime[sx.center.scanCenter])
					+ ", intensity="
					+ sx.center.intensityCenter
					+ ", probes=" + sx.size()
					+ ", data scans="
					+ sx.getMaxDatumScans()
					+ ", cont scans="
					+ sx.getContigousMaxDatumScans()
					+ ", cont ratio="
					+ sx.getContigousToMaxDatumScansRatio());
		}
	    } else {
		if (sx.size() > 0) {
		    if (debug > 0)
			System.out
				.println("Ignored by time range criteria: id="
					+ sx.spotId
					+ " "
					+ mzFormat.format(sx.center.mzCenter)
					+ " mz, time="
					+ timeFormat
						.format(retentiontime[sx.center.scanCenter])
					+ ", intensity="
					+ sx.center.intensityCenter
					+ ", probes=" + sx.size()
					+ ", data scans="
					+ sx.getMaxDatumScans()
					+ ", cont scans="
					+ sx.getContigousMaxDatumScans()
					+ ", cont ratio="
					+ sx.getContigousToMaxDatumScansRatio());
		}
	    }
	    setProcedure(i++, spots.size(), 9);
	}
	logger.info("Peaks on " + dataFile + " = "
		+ newPeakList.getNumberOfRows());

	// Add new peaklist to the project
	project.addPeakList(newPeakList);

        // Add quality parameters to peaks
	QualityParameters.calculateQualityParameters(newPeakList);

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished chromatogram builder (RT) on " + dataFile);

    }

    public double intensityRatio(double int1, double int2) {
	return Math.min(int1, int2) / Math.max(int1, int2);
    }

    public void setProcedure(int i, int max, float process) {
	float procedureLen = 10.0f;
	procedure = (process + (float) i / (float) max) / procedureLen;
    }

    public IndexedDataPoint[][] smoothDataPoints(RawDataFile dataFile,
	    double timeSpan, double timeMZSpan, int scanSpan, double mzTol,
	    int mzPoints, double minimumHeight) {
	int[] scanNumbers = dataFile.getScanNumbers(1);
	int totalScans = scanNumbers.length;
	DataPoint mzValues[][] = null; // [relative scan][j value]
	DataPoint mzValuesJ[] = null;
	int mzValuesScan[] = null;
	int mzValuesMZidx[] = null;
	IndexedDataPoint newMZValues[][] = null;
	IndexedDataPoint tmpDP[] = new IndexedDataPoint[0];
	newMZValues = new IndexedDataPoint[totalScans][];
	int i, j, si, sj, ii, k, ssi, ssj, m;
	double timeSmoothingMZtol = Math.max(timeMZSpan, 1e-6);

	int modts = Math.max(1, totalScans / 10);

	for (i = 0; i < totalScans; i++) {

	    if (i % 100 == 0 && isCanceled())
		return null;

	    // Smoothing in TIME space
	    Scan scan = dataFile.getScan(scanNumbers[i]);
	    double rt = retentiontime[i];
	    DataPoint[] xDP = null;
	    IndexedDataPoint[] iDP = null;
	    sj = si = i;
	    ssi = ssj = i;
	    int t = 0;
	    if (timeSpan > 0 || scanSpan > 0) {
		if (scan != null) {
		    for (si = i; si > 1; si--) {
			if (retentiontime[si - 1] < rt - timeSpan / 2) {
			    break;
			}
		    }
		    for (sj = i; sj < totalScans - 1; sj++) {
			if (retentiontime[sj + 1] >= rt + timeSpan / 2) {
			    break;
			}
		    }
		    ssi = i - (scanSpan - 1) / 2;
		    ssj = i + (scanSpan - 1) / 2;
		    if (ssi < 0) {
			ssj += -ssi;
			ssi = 0;
		    }
		    if (ssj >= totalScans) {
			ssi -= (ssj - totalScans + 1);
			ssj = totalScans - 1;
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
			mzValuesScan = new int[sj - si + 1];
			mzValuesMZidx = new int[sj - si + 1];
		    }
		    // Load Data Points
		    for (j = si; j <= sj; j++) {
			int jsi = j - si;
			if (mzValues[jsi] == null
				|| jsi >= mzValuesScan.length - 1
				|| mzValuesScan[jsi + 1] != scanNumbers[j]) {
			    Scan xscan = dataFile.getScan(scanNumbers[j]);
			    mzValues[jsi] = xscan.getDataPoints();
			    mzValuesScan[jsi] = scanNumbers[j];
			} else {
			    mzValues[jsi] = mzValues[jsi + 1];
			    mzValuesScan[jsi] = mzValuesScan[jsi + 1];
			}
			mzValuesMZidx[jsi] = 0;
		    }
		    // Estimate Averages
		    ii = i - si;
		    if (tmpDP.length < mzValues[ii].length)
			tmpDP = new IndexedDataPoint[mzValues[ii].length * 3 / 2];
		    for (k = 0; k < mzValues[ii].length; k++) {
			DataPoint dp = mzValues[ii][k];
			double mz = dp.getMZ();
			double intensidad = 0;
			if (dp.getIntensity() > 0) { // only process those > 0
			    double a = 0;
			    short c = 0;
			    int f = 0;
			    for (j = 0; j <= sj - si; j++) {
				for (mzValuesJ = mzValues[j]; mzValuesMZidx[j] < mzValuesJ.length - 1
					&& mzValuesJ[mzValuesMZidx[j] + 1]
						.getMZ() < mz
						- timeSmoothingMZtol; mzValuesMZidx[j]++)
				    ;

				f = mzValuesMZidx[j];

				for (m = mzValuesMZidx[j] + 1; m < mzValuesJ.length
					&& mzValuesJ[m].getMZ() < mz
						+ timeSmoothingMZtol; m++) {
				    if (Math.abs(mzValuesJ[m].getMZ() - mz) < Math
					    .abs(mzValuesJ[f].getMZ() - mz)) {
					f = m;
				    } else {
					// siempre debe ser mas cercano porque
					// están ordenados por masa, entonces
					// parar la búsqueda
					break;
				    }
				}
				if (f > 0
					&& f < mzValuesJ.length
					&& Math.abs(mzValuesJ[f].getMZ() - mz) <= timeSmoothingMZtol
					&& mzValuesJ[f].getIntensity() > 0) { // >=
				    // minimumHeight
				    // ?
				    // System.out.println("mz="+mz+"; Closer="+mzValuesJ[f].getMZ()+", f="+f+", Intensity="+mzValuesJ[f].getIntensity());
				    a += mzValuesJ[f].getIntensity();
				    c++;
				}
			    }
			    intensidad = c > 0 ? a / c : 0;
			    if (intensidad >= minimumHeight) {
				tmpDP[t++] = new IndexedDataPoint(k,
					new SimpleDataPoint(mz, intensidad));
			    }
			}
		    }

		}
	    } else if (scan != null) {
		xDP = scan.getDataPoints();
		if (tmpDP.length < xDP.length)
		    tmpDP = new IndexedDataPoint[xDP.length];
		for (k = 0; k < xDP.length; k++) {
		    if (xDP[k].getIntensity() >= minimumHeight) {
			tmpDP[t++] = new IndexedDataPoint(k, xDP[k]);
		    }
		}
	    }
	    iDP = new IndexedDataPoint[t];
	    for (k = 0; k < t; k++) {
		iDP[k] = tmpDP[k];
	    }
	    newMZValues[i] = iDP;

	    setProcedure(i, totalScans, 0);

	    if (i % modts == 0) {
		logger.info("Smoothing/Caching " + dataFile + "..."
			+ (i / modts) * 10 + "%");
	    }

	}

	return newMZValues;
    }

    public double HWHM(double x0, double x1, double y0, double y1) {
	// x0 is the "scan" or m/z estimated at the highest peak
	// y0 is the "highest" peak intensity
	// x1 is the "scan" or m/z estimated at the point closest to (highest
	// intensity / 2)
	// y1 is the intensity closest to (highest intensity / 2)
	// x1_2 = x0 - F * (x0-x1)
	// F = (y0 - y0/2) / (y0 - y1) = y0/2 / (y0 - y1)
	// x1_2 = x0(1-F) + x1*F /// this X1/2 is given a triangle, so using the
	// same slope than between x0 and x1, this x1_2 is farther than the
	// actual 1/2 point
	// x3_4 = (x1_2 - x1) / 2 + x1 // aproximation to real X 1/2 by the half
	// of the difference between the x1 and the estimated x1_2
	double f = (y0 / 2) / (y0 - y1);
	if (Double.isInfinite(f))
	    f = 1;
	double x3_4 = (x1 * (1 + f) / 2) - (x0 * (1 + f) / 2);
	return Math.abs(x3_4);
    }

    int addMaxDatumFromScans(SpotByProbes s, Chromatogram peak) {

	int i, j;
	int adds = 0;
	for (i = s.minScan; i <= s.maxScan; i++) {
	    Datum[] di = roi[i];
	    if (di != null && di.length > 0) {
		Datum max = new Datum(new SimpleDataPoint(0, -1), 0,
			new SimpleDataPoint(0, -1));
		int idx = findFirstMass(s.minMZ, di);
		for (j = idx; j < di.length && di[j].mz <= s.maxMZ; j++) {
		    Datum d = di[j];
		    if (d.spotId == s.spotId) {
			if (d.intensity > max.intensity && d.mz >= s.minMZ
				&& d.intensity > minimumHeight) {
			    max = d;
			}
		    }
		}
		if (max.intensity > 0) {
		    adds++;
		    peak.addMzPeak(scans[i].getScanNumber(), new SimpleDataPoint(
			    max.mzOriginal, max.intensityOriginal));
		}
	    }
	}
	return adds;
    }

    void assignSpotIdToDatumsFromScans(SpotByProbes s, int sRadius,
	    double mzRadius) {

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
			    if (p.mzCenter == s.center.mzCenter
				    && p.scanCenter == s.center.scanCenter) {
				// This datum is actually MINE (s) !!!, this
				// will happen to datums close to spot borders
				// and that compete with other spot
				// System.out.println("Reassigning spot to Id="+s.spotId+" from Spot:"+d.toString());
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

    void assignSpotIdToDatumsFromSpotId(SpotByProbes s, SpotByProbes s2,
	    int sRadius, double mzRadius) {

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
	Datum max = new Datum(new SimpleDataPoint(0, -1), 0,
		new SimpleDataPoint(0, -1));
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
	    if (max.intensity >= 0
		    && (max.mz != p.mzCenter || max.scan != p.scanCenter)) {
		p.mzCenter = max.mz;
		p.scanCenter = max.scan;
		p.intensityCenter = max.intensity;
		// p.moves++;
	    } else {
		move = false;
	    }
	}
    }

    double intensityForMZorScan(ArrayList<DatumExpand> deA, double mz, int scan) {
	double h = -1;
	int j;
	for (j = 0; j < deA.size(); j++) {
	    DatumExpand de = deA.get(j);
	    if ((de.dato.scan == scan || de.dato.mz == mz)
		    && de.dato.intensity > h) {
		h = de.dato.intensity;
	    }
	}
	return h;
    }

    double[] massCenter(int l, int r, double min, double max) {
	double x = 0;
	double y = 0;
	double sum = 0;
	double maxValue = 0;
	for (int i = l; i <= r; i++) {
	    DataPoint mzs[] = getCachedDataPoints(scans[i].getScanNumber());
	    if (mzs != null) {
		for (int j = findFirstMass(min, mzs); j < mzs.length; j++) {
		    double mass = mzs[j].getMZ();
		    if (mass >= min) {
			if (mass <= max) {
			    double intensity = mzs[j].getIntensity();
			    if (intensity >= minimumHeight) {
				x += i * intensity;
				y += mass * intensity;
				sum += intensity;
				if (intensity > maxValue) {
				    maxValue = intensity;
				}
			    }
			} else {
			    break;
			}
		    }
		}
	    }
	}
	if (sum > 0.0) {
	    x /= sum;
	    y /= sum;
	}
	return new double[] { x, y, maxValue };
    }

    static int findFirstMass(double mass, DataPoint mzValues[]) {
	int l = 0;
	int r = mzValues.length - 1;
	int mid = 0;
	while (l < r) {
	    mid = (r + l) / 2;
	    if (mzValues[mid].getMZ() > mass) {
		r = mid - 1;
	    } else if (mzValues[mid].getMZ() < mass) {
		l = mid + 1;
	    } else {
		return mid;
	    }
	}
	while (l > 0 && mzValues[l].getMZ() > mass)
	    l--;
	return l;
    }

    static int findFirstMass(double mass, Datum mzValues[]) {
	return findFirstMass(mass, mzValues, 0, mzValues.length - 1);
    }

    static int findFirstMass(double mass, Datum mzValues[], int l, int r) {
	int mid = 0;
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
	while (l > 0 && mzValues[l].mz > mass)
	    l--;
	return l;
    }

    Spot intensities(int l, int r, double min, double max, Chromatogram chr,
	    PearsonCorrelation stats, int spotId) {
	boolean passSpot = false;
	Spot s = new Spot();
	if (r >= scans.length)
	    r = scans.length - 1;
	if (l < 0)
	    l = 0;
	for (int i = l; i <= r; i++) {
	    Datum mzs[] = roi[i];
	    if (mzs != null) {
		Datum mzMax = null;
		for (int j = findFirstMass(min, mzs); j < mzs.length; j++) {
		    double mass = mzs[j].mz;
		    double mjint = mzs[j].intensity;
		    if (mass >= min) {
			if (mass <= max) {
			    if (mzs[j].spotId == spotId || passSpot) {
				s.addPoint(i, mass,
					(mjint >= minimumHeight ? mjint
						: -mjint));
				if (mjint >= minimumHeight) {
				    if (mzMax == null
					    || mjint > mzMax.intensity) {
					mzMax = mzs[j];
				    }
				}
			    } else {
				if (mjint >= minimumHeight)
				    s.pointsNoSpot++;
			    }
			} else {
			    break;
			}
		    }
		}
		if (chr != null && mzMax != null) {
		    // Add ONLY THE MAX INTENSITY PER SCAN
		    chr.addMzPeak(scans[i].getScanNumber(), new SimpleDataPoint(mzMax.mz,
			    mzMax.intensity)); // mzMax
		}
		if (stats != null && mzMax != null) {
		    stats.enter(i, mzMax.mz);
		}
	    }
	}
	return s;
    }

    DataPoint[] getCachedDataPoints(int scan) {
	if (dpCache == null)
	    dpCache = new HashMap<Integer, DataPoint[]>();
	DataPoint[] dp = dpCache.get(scan);
	if (dp != null) {
	    return dp;
	}
	Scan s = dataFile.getScan(scan);
	if (s == null)
	    return null;
	dp = s.getDataPoints();
	dpCache.put(scan, dp);
	return dp;
    }

}
