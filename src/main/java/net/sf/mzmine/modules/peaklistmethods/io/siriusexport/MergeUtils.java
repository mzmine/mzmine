package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import org.apache.commons.math3.special.Erf;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
    Merge several MS/MS scans into one
 */
public class MergeUtils {

    /**
     * Peaks below this threshold will not be counted as chimerics. The reasoning is that peaks with low intensity
     * won't contaminate the MS/MS scans anyways.
     */
    public static final double CHIMERIC_INTENSITY_THRESHOLD = 0.1;
    /**
     * The masslist to use. If null, always pick all peaks
     */
    protected String masslist;

    /**
     * expected average mass deviation of fragment peaks
     */
    protected double expectedPPM;

    /**
     * width and offset (from precursor m/z) of the isolation window.
     * Note that the window is centered at the precursor m/z
     */
    protected Range<Double> isolationWindow;

    /**
     * Only merge spectra if their cosine is above this threshold.
     * Important: If your spectra come from different collision energies, you might want to set this threshold very low!
     */
    protected double cosineThreshold;

    /**
     * If true, the fragment m/z is calculated as weighted average over the merged fragment peaks
     * If false, the fragment m/z is the m/z of the most intensive fragment peak.
     *
     * I guess first option may sometimes have more accurate m/z, while latter one is more safe and will rarely fail
     */
    protected boolean mergeMasses;

    /**
     * Only merge spectra if their MS1 precursor intensity is above this threshold (relative to most intensive precursor
     * peak).
     */
    protected double minPrecursorIntensityRelativeToBestPeak;

    /**
     * Remove spectra which chimeric contamination is above this threshold
     */
    protected double maximalChimericIntensity;


    /**
     * do not merge if the chimeric contamination is larger than X times the contamination of the best peak
     */
    protected double maximalChimericContaminationRelativeToBestPeak;

    /**
     * Remove peaks if they are contained in less than X % of the merged spectra.
     */
    protected double peakRemovalThreshold;

    /**
     * do not merge if the chimeric contamination is larger than X times the absolute contamination of the best peak
     */
    protected double maximalAbsoluteChimericContaminationRelativeToBestPeak;

    /**
     * When merging masses, remove outliers from left and right
     */
    protected boolean cutoffOutliers;

    /**
     * Only consider peaks for cosine calculation which are above this intensity level
     */
    protected double intensityCutoffForCosine;

    protected MergedStatistics statistics;

    public MergeUtils() {
        this.expectedPPM = 10;
        this.isolationWindow = Range.closed(-1d,1d);
        this.cosineThreshold = 0.8;
        this.mergeMasses = true;
        this.masslist = null;
        this.minPrecursorIntensityRelativeToBestPeak = 0.25;
        this.maximalChimericIntensity = 0.66;
        this.maximalChimericContaminationRelativeToBestPeak = 1.33;
        this.maximalAbsoluteChimericContaminationRelativeToBestPeak = 1.33;
        this.peakRemovalThreshold = 0d;
        this.cutoffOutliers = true;
        this.intensityCutoffForCosine = 0d;
        this.statistics = new MergedStatistics();
    }

    public double getIntensityCutoffForCosine() {
        return intensityCutoffForCosine;
    }

    public void setIntensityCutoffForCosine(double intensityCutoffForCosine) {
        this.intensityCutoffForCosine = intensityCutoffForCosine;
    }

    public double getPeakRemovalThreshold() {
        return peakRemovalThreshold;
    }

    public void setPeakRemovalThreshold(double peakRemovalThreshold) {
        this.peakRemovalThreshold = peakRemovalThreshold;
    }

    public boolean isCutoffOutliers() {
        return cutoffOutliers;
    }

    public void setCutoffOutliers(boolean cutoffOutliers) {
        this.cutoffOutliers = cutoffOutliers;
    }

    public String getMasslist() {
        return masslist;
    }

    public void setMasslist(String masslist) {
        this.masslist = masslist;
    }

    public double getExpectedPPM() {
        return expectedPPM;
    }

    public void setExpectedPPM(double expectedPPM) {
        this.expectedPPM = expectedPPM;
    }

    public Range<Double> getIsolationWindow() {
        return isolationWindow;
    }

    public void setIsolationWindow(Range<Double> isolationWindow) {
        this.isolationWindow = isolationWindow;
    }

    public double getCosineThreshold() {
        return cosineThreshold;
    }

    public void setCosineThreshold(double cosineThreshold) {
        this.cosineThreshold = cosineThreshold;
    }

    public boolean isMergeMasses() {
        return mergeMasses;
    }

    public void setMergeMasses(boolean mergeMasses) {
        this.mergeMasses = mergeMasses;
    }

    protected double getTIC(DataPoint[] scan, double parentPeakThreshold) {
        double TIC = 0d;
        for (DataPoint p : scan) {
            if (p.getMZ() > parentPeakThreshold)
                break;
            TIC += p.getIntensity();
        }
        return TIC;
    }


    //////////////////////////////////////////////////////////////////


    /**
     * Merge a list of scans from different origins into one spectrum. Each peak in the merged spectrum remembers its original
     * peaks.
     * @param name just for debugging purposes: some name string to identify the origin of the fragment scan
     * @param parentPeak the m/z of the precursor ion
     * @param scans a list of scans which should be merged
     * @return merged spectrum
     */
    public MergedSpectrum mergeScansFromDifferentOrigins(String name, double parentPeak, List<FragmentScan> scans) {
        if (scans.isEmpty()) return MergedSpectrum.empty();
        // first split of list by origins
        final HashMap<String, List<FragmentScan>> byName = new HashMap<>();
        for (FragmentScan s : scans) {
            byName.computeIfAbsent(s.origin.getName(), (x) -> new ArrayList<FragmentScan>()).add(s);
        }
        final List<List<FragmentScan>> allFs = new ArrayList<>(byName.values().stream().filter(x->!x.isEmpty()).collect(Collectors.toList()));
        if (allFs.size()==1) {
            return mergeConsecutiveScans(name, parentPeak,allFs.get(0), true);
        }
        // sort the scans by their best scan
        allFs.sort(Comparator.comparingDouble((List<FragmentScan> x)->x.stream().max(Comparator.comparingDouble(FragmentScan::getScore)).get().getScore()).reversed());
        // now merge spectrum by spectrum. Use cosine threshold between merged spectra
        MergedSpectrum left = mergeConsecutiveScans(name,parentPeak,allFs.get(0),true);
        final double bestTIC = left.getTIC();
        statistics.addTic(left.getTIC());
        if (left.data.length==0) return MergedSpectrum.empty();
        final CosineSpectrum leftCosine = new CosineSpectrum(left.data,parentPeak, expectedPPM);
        for (int k=1; k < allFs.size(); ++k) {
            MergedSpectrum right = mergeConsecutiveScans(name,parentPeak,allFs.get(k),true);
            final CosineSpectrum rightCosine = new CosineSpectrum(right.data, parentPeak,expectedPPM);
            final double cosine =leftCosine.cosine(rightCosine);
            double tic = right.getTIC();
            if (cosine>=cosineThreshold) {
                Arrays.sort(right.data, CompareDataPointsByDecreasingInt);
                left = left.merge(right, merge(left.data, right.data));
                statistics.addTic(tic);;
                continue;
            } else if (cosine < 0.33) {
                // check data quality
                if (tic < 0.2*bestTIC || tic < statistics.get10PercentileTIC()) {
                    // we might just get a low intensive specturm. May happen.
                    left.removedScansByLowQuality += right.totalNumberOfScans();
                    continue;
                }
                statistics.incrementLowCosine();
                final double lowRes = lowResolutionCosine(left.data, right.data, parentPeak-1.5);
                if (lowRes >= cosineThreshold && lowRes >= (0.2+cosine)) {
                    statistics.warnForLowCosine(name + ": Low highres cosine score of " + cosine + " for aligned features, while lowres cosine is still " + lowRes + ". For " + left.toString() + " and " + right.toString() + ". Please check your expected mass deviation settings. It might be too low.", tic);
                    statistics.incrementLowResHighResDifference();
                } else {
                    statistics.warnForLowCosine(name + ": Low cosine score of " + cosine + " for aligned features from " + left.toString() + " and " + right.toString(), tic);
                }
            }
            left.removedScansByLowCosine += right.totalNumberOfScans();
        }
        statistics.flushWarningQueue();
        return filterOutRarePeaks(this.cutoffOutliers ? cutOffOutliers(left) : left);

    }

    public MergedSpectrum filterOutRarePeaks(MergedSpectrum mergedSpectrum) {
        int minimumNumberOfScans = (int)Math.floor(this.peakRemovalThreshold*mergedSpectrum.scanIds.length);
        if (minimumNumberOfScans <= 1) return mergedSpectrum;
        else return mergedSpectrum.filterByNumberOfScans(minimumNumberOfScans);
    }

    private MergedSpectrum cutOffOutliers(MergedSpectrum mergedSpectrum) {
        if (!this.isMergeMasses()) return mergedSpectrum;
        for (int k=0; k < mergedSpectrum.data.length; ++k) {
            if (mergedSpectrum.data[k].sources.length >= 4) {
                mergedSpectrum.data[k] = mergedSpectrum.data[k].cutoffOutliers();
            }
        }
        return mergedSpectrum;
    }

    /**
     * Removes all MS/MS scans from the list which do not satisfy the threshold conditions
     */
    public List<DataPoint[]> extractHighQualityScans(double parentPeak, List<FragmentScan> scans, List<Integer> extractedScanNumbers) {
        List<DataPoint[]> dps = new ArrayList<>();
        findhighQualityScans(parentPeak, scans, dps,extractedScanNumbers);
        return dps;
    }

    /**
     * Just make some statistics about the TIC in MS/MS spectra to get a "feeling" about what is a good and what a
     * bad spectrum
     * @param scans some randomly selected MS/MS scans
     */
    public void prefillStatisticsWithTICExamples(Iterable<Scan> scans) {
        for (Scan scan : scans) {
            double tic=0d;
            for (DataPoint p : scan.getDataPoints()) {
                tic += p.getIntensity();
            }
            statistics.addTic(tic);
        }
    }

    /**
     * Merge a list of consecutive scans into one spectrum. Each peak in the merged spectrum remembers its original
     * peaks.
     * @param name just for debugging purposes: some name string to identify the origin of the fragment scan
     * @param parentPeak the m/z of the precursor ion
     * @param scans a list of scans which should be merged
     * @return merged spectrum
     */
    protected MergedSpectrum mergeConsecutiveScans(String name, double parentPeak, List<FragmentScan> scans) {
        return mergeConsecutiveScans(name,parentPeak,scans,false);
    }

    protected MergedSpectrum mergeConsecutiveScans(String name, double parentPeak, List<FragmentScan> scans, boolean innerMerge) {
        if (scans.isEmpty()) return MergedSpectrum.empty();
        // we start with the highest TIC and then merge in both directions
        boolean usecosine = cosineThreshold > 0;

        // just to ensure that our scans are all ordered by m/z.
        final List<DataPoint[]> mzOrderedCopy = new ArrayList<>();

        // sort scans such that consecutive scans are always next to each other
        scans.sort(Comparator.comparing((FragmentScan x)->x.origin.getName()).thenComparing(x->x.ms2ScanNumbers[0]));

        // add all high quality scans to mzOrderedCopy and return index of best scan
        final List<Integer> scanNumbers = new ArrayList<>();
        int bestScan = findhighQualityScans(parentPeak, scans, mzOrderedCopy, scanNumbers);
        if (mzOrderedCopy.isEmpty()||bestScan<0) return MergedSpectrum.empty();

        // calculate highres cosine normalization factor
        final CosineSpectrum[] cosines = new CosineSpectrum[mzOrderedCopy.size()];
        for (int k = 0; k < mzOrderedCopy.size(); ++k) {
            if (usecosine) {
                cosines[k] = new CosineSpectrum(mzOrderedCopy.get(k), parentPeak, expectedPPM);
            }
        }

        // remember how many scans we had initially
        int numberOfTotalScans = 0;
        for (FragmentScan f : scans) numberOfTotalScans += f.ms2ScanNumbers.length;

        final int numberOfScansRemovedDueToLowQuality = numberOfTotalScans - mzOrderedCopy.size();
        final List<Integer> usedScanNumbers = new ArrayList<>();
        // now, start from best scan and merge in both direction if cosine is large enough. For simplicity, we only
        // compute cosine between the original scans, not the merged scan. This is slightly more efficient.
        int numberOfMerges = 1;
        DataPoint[] dataPoints = mzOrderedCopy.get(bestScan);
        usedScanNumbers.add(scanNumbers.get(bestScan));
        MergeDataPoint[] mergedSpectrum = new MergeDataPoint[dataPoints.length];
        {
            for (int k = 0; k < dataPoints.length; ++k) mergedSpectrum[k] = new MergeDataPoint(dataPoints[k]);
        }

        int i = bestScan - 1, j = bestScan + 1;
        while (i > 0 || j < mzOrderedCopy.size()) {
            if (i > 0) {
                MergeDataPoint[] merged = merge(mzOrderedCopy.get(bestScan), mergedSpectrum, mzOrderedCopy.get(i), cosines[bestScan], cosines[i], cosineThreshold, parentPeak);
                if (merged != null) {
                    mergedSpectrum = merged;
                    ++numberOfMerges;
                    usedScanNumbers.add(scanNumbers.get(i));
                }
                --i;
            }
            if (j < mzOrderedCopy.size()) {
                MergeDataPoint[] merged = merge(mzOrderedCopy.get(bestScan), mergedSpectrum, mzOrderedCopy.get(j), cosines[bestScan], cosines[j], cosineThreshold, parentPeak);
                if (merged != null) {
                    mergedSpectrum = merged;
                    ++numberOfMerges;
                    usedScanNumbers.add(scanNumbers.get(j));
                }
                ++j;
            }
        }

        final int numberOfScansRemovedDueToLowCosine = mzOrderedCopy.size()-numberOfMerges;

        // write a warning if too many scans are filtered out due to low cosine
        if (mzOrderedCopy.size() >= 4 && ((double) numberOfMerges) / mzOrderedCopy.size() < 0.5) {
            LoggerFactory.getLogger(MergeUtils.class).warn(name + ": Only " + numberOfMerges + " of " + mzOrderedCopy.size() + " spectra are merged. All other have a too low cosine similarity. Cosines between most intensive scan and other scans are: " + Arrays.toString(cosines));
        }
        MergedSpectrum M = new MergedSpectrum(mergedSpectrum, new RawDataFile[]{scans.get(0).origin}, scanNumbers.stream().mapToInt(x -> x).toArray(), numberOfScansRemovedDueToLowQuality, numberOfScansRemovedDueToLowCosine);
        if (cutoffOutliers) M = cutOffOutliers(M);
        if (!innerMerge) M = filterOutRarePeaks(M);
        return M;
    }

    /**
     * extract all high quality scans from the list that satisfy the threshold conditions. Adds them to the provided
     * list. Returns index of best quality scan
     * @param parentPeak precursor m/z
     * @param scans list of scans
     * @param mzOrderedCopy an empty list for the results
     * @return index of best scan from the result list
     */
    private int findhighQualityScans(double parentPeak, List<FragmentScan> scans, List<DataPoint[]> mzOrderedCopy, List<Integer> extractedScanNumbers) {
        // find best scan, which is scan with high MS1 intensity, low chimeric contamination and high TIC
        int bestScan = 0;
        {
            // search best scan
            int best = 0; double bestScore = Double.NEGATIVE_INFINITY; double highestInt=0d;
            for (int k=0; k < scans.size(); ++k) {
                highestInt = scans.get(k).precursorIntensity;
                double score = scans.get(k).getScore();
                if (score >bestScore) {
                    bestScore = score;
                    best = k;
                }
            }
            int bestOffset=0;
            final FragmentScan bestScanF = scans.get(best);
            for (FragmentScan scan : scans) {
                int o=0, bo=0;
                double bestTIC = Double.NEGATIVE_INFINITY;
                if (scan== bestScanF) {
                    for (int k=0; k < scan.ms2ScanNumbers.length; ++k) {
                        final Scan s = scan.origin.getScan(scan.ms2ScanNumbers[k]);
                        DataPoint[] dps = useMassList(s);
                        double tic = getTIC(dps, parentPeak-1.5);
                        if (tic==0)
                            continue;
                        mzOrderedCopy.add(dps);
                        if (extractedScanNumbers!=null) extractedScanNumbers.add(scan.ms2ScanNumbers[k]);
                        if (tic > bestTIC) {
                            bo=o;
                            bestTIC = tic;
                        }
                        ++o;
                    }
                    if (bestTIC <= 0) {
                        LoggerFactory.getLogger(MergeUtils.class).warn("Fragment spectrum with good MS1 has empty MS/MS spectrum. MS1 SCAN ID is " + scan.ms1ScanNumber + ", MS/MS SCAN ID are " + Arrays.toString(scan.ms2ScanNumbers));
                        return -1;
                    }
                    bestScan = bestOffset + bo;
                } else {
                    // check for thresholds
                    final double ms1Int = scan.precursorIntensity/highestInt;
                    final double chimericInt = (scan.getChimericIntensityWithPseudocount()/scan.precursorIntensity)/(bestScanF.getChimericIntensityWithPseudocount()/bestScanF.precursorIntensity);
                    final boolean lowChimericIntensiy = scan.chimericIntensity <= scan.precursorIntensity*0.1;
                    if ((ms1Int >= minPrecursorIntensityRelativeToBestPeak) && (lowChimericIntensiy || (maximalChimericContaminationRelativeToBestPeak >= chimericInt && maximalChimericIntensity >= scan.chimericIntensity/scan.precursorIntensity ))) {
                        for (int id : scan.ms2ScanNumbers) {
                            DataPoint[] spectrum = useMassList(scan.origin.getScan(id));
                            if (spectrum.length==0)
                                continue;
                            mzOrderedCopy.add(spectrum);
                            if (extractedScanNumbers!=null) extractedScanNumbers.add(id);
                            ++bestOffset;
                        }
                    }
                }

            }
        }
        return bestScan;
    }

    /**
     * Use peaks from the given mass list, or all peaks if mass list is null
     */
    private DataPoint[] useMassList(Scan s) {
        if (masslist==null) {
            return orderIfNotOrdered(s.getDataPoints());
        } else {
            MassList massList = s.getMassList(masslist);
            if (massList==null)
                throw new NullPointerException("No masslist with name " + masslist + " in scan " + s.getScanNumber() + ", m/z = " + s.getPrecursorMZ() + ", retention time = " + s.getRetentionTime());
            return orderIfNotOrdered(massList.getDataPoints());
        }
    }

    private static DataPoint[] orderIfNotOrdered(DataPoint[] scan) {
        for (int k = 1; k < scan.length; ++k) {
            if (scan[k].getMZ() < scan[k - 1].getMZ()) {
                DataPoint[] copy = scan.clone();
                Arrays.sort(copy, CompareDataPointsByMz);
                return copy;
            }
        }
        return scan;
    }

    /**
     * Internal method. Merge spectra if cosine is above cosine threshold
     * @return
     */
    protected MergeDataPoint[] merge(DataPoint[] origin, MergeDataPoint[] left, DataPoint[] right, CosineSpectrum cosineLeft, CosineSpectrum cosineRight, double cosineThreshold, double parentPeak) {
        if (cosineLeft.norm==0 || cosineRight.norm==0) return null;
        if (cosineThreshold > 0) {
            double cosine = cosineLeft.cosine(cosineRight);
            final double oldcs = cosine;
            if (cosine < cosineThreshold) {
                if (cosine < 0.25)
                    LoggerFactory.getLogger(MergeUtils.class).warn("Detect a very low cosine between two fragment scans: " + cosine  + " for precursor m/z = " + parentPeak);
                return null;
            }
        }
        // sort right list by intensity
        final DataPoint[] rightInt = right.clone();
        Arrays.sort(rightInt, CompareDataPointsByDecreasingInt);
        return merge(left, right);
    }

    /**
     * Merge a scan into a merged spectrum.
     * @param orderedByMz peaks from merged spectrum, sorted by ascending m/z
     * @param orderedByInt peaks from scan, sorted by ascending intensity
     * @return a merged spectrum. Might be the original one if no new peaks were added.
     */
    protected MergeDataPoint[] merge(MergeDataPoint[] orderedByMz, DataPoint[] orderedByInt) {
        // we assume a rather large deviation as signal peaks should be contained in more than one
        // measurement
        final List<MergeDataPoint> append = new ArrayList<>();
        final double absoluteDeviation = 400 * expectedPPM * 1e-6;
        for (int k = 0; k < orderedByInt.length; ++k) {
            final DataPoint peak = orderedByInt[k];
            final double dev = Math.max(absoluteDeviation, peak.getMZ() * 4 * expectedPPM * 1e-6);
            final double lb = peak.getMZ() - dev, ub = peak.getMZ() + dev;
            int mz1 = Arrays.binarySearch(orderedByMz, peak, CompareDataPointsByMz);
            if (mz1 < 0) {
                mz1 = -(mz1 + 1);
            }
            int mz0 = mz1 - 1;
            while (mz1 < orderedByMz.length && orderedByMz[mz1].getMZ() <= ub)
                ++mz1;
            --mz1;
            while (mz0 >= 0 && orderedByMz[mz0].getMZ() >= lb)
                --mz0;
            ++mz0;
            if (mz0 <= mz1) {
                // merge!
                int mostIntensive = mz0;
                double bestScore = Double.NEGATIVE_INFINITY;
                for (int i = mz0; i <= mz1; ++i) {
                    final double massDiff = orderedByMz[i].getMZ() - peak.getMZ();
                    final double score =
                            Erf.erfc(3 * massDiff) / (dev * Math.sqrt(2)) * orderedByMz[i].getIntensity();
                    if (score > bestScore) {
                        bestScore = score;
                        mostIntensive = i;
                    }
                }

                orderedByMz[mostIntensive] = orderedByMz[mostIntensive].merge(peak, mergeMasses);

            } else {
                // append
                append.add(new MergeDataPoint(peak));
            }
        }
        if (append.size() > 0) {
            int offset = orderedByMz.length;
            orderedByMz = Arrays.copyOf(orderedByMz, orderedByMz.length + append.size());
            for (MergeDataPoint p : append) {
                orderedByMz[offset++] = p;
            }
            Arrays.sort(orderedByMz, CompareDataPointsByMz);
        }
        return orderedByMz;
    }

    public static double lowResolutionCosine(DataPoint[] left, DataPoint[] right, double maxMz) {
        return lowResolutionDotProduct(left,right,maxMz)/Math.sqrt(lowResolutionDotProduct(left,left,maxMz)*lowResolutionDotProduct(right,right,maxMz));
    }
    public static double lowResolutionDotProduct(DataPoint[] left, DataPoint[] right, double maxMz) {
        int i=0, j=0;
        double binL = 0d, binR = 0d;
        long bin = 0;
        double score = 0d;
        long maxBin = Math.round(maxMz*10);
        while (bin < maxBin) {
            long binLeft=maxBin, binRight=maxBin;
            if (i < left.length) {
                binLeft = Math.round(left[i].getMZ()*10);
                if (binLeft==bin) {
                    binL += left[i].getIntensity();
                    ++i;
                } else if (binLeft < bin)
                    ++i;
            }
            if (j < right.length) {
                binRight = Math.round(right[j].getMZ()*10);

                if (binRight==bin) {
                    binR += right[j].getIntensity();
                    ++j;
                } else if (binRight < bin)
                    ++j;
            }
            if (binRight != bin && binLeft != bin) {
                bin = Math.max(binLeft,binRight);
                score += binL*binR;
                binL=0d; binR=0d;
            }
        }
        return score;

    }

    /**
     * Calculate the integral product between two spectra. Highres version of the well known dot product.
     * To get a cosine score, you have to normalize this score!
     * @param left first spectrum
     * @param right second spectrum
     * @param expectedPPM expected average mass deviation
     * @param maxMz usually the parent mass. Peaks behind this value are ignored
     * @return
     */
    public static double probabilityProduct(DataPoint[] left, DataPoint[] right, double expectedPPM, double maxMz, double intensityCutoff) {
        int i = 0, j = 0;
        double score = 0d;
        final double allowedDifference = Math.min(1, 1000 * expectedPPM * 5e-6);
        final int nl, nr;//=left.length, nr=right.length;
        {
            int maxi = Arrays.binarySearch(left, new SimpleDataPoint(maxMz + 0.5d, 0), CompareDataPointsByMz);
            if (maxi < 0) maxi = -(maxi + 1);
            nl = maxi;
            maxi = Arrays.binarySearch(right, new SimpleDataPoint(maxMz + 0.5d, 0), CompareDataPointsByMz);
            if (maxi < 0) maxi = -(maxi + 1);
            nr = maxi;
        }

        while (i < nl && j < nr) {
            DataPoint lp = left[i];
            if (lp.getIntensity() < intensityCutoff) {
                ++i;
                continue;
            }
            DataPoint rp = right[j];
            if (rp.getIntensity() < intensityCutoff) {
                ++j;
                continue;
            }
            final double difference = lp.getMZ() - rp.getMZ();
            if (Math.abs(difference) <= allowedDifference) {
                final double mzabs = Math.max(200 * expectedPPM * 1e-6, expectedPPM * 1e-6 * Math.round(lp.getMZ() + rp.getMZ()) / 2d);
                final double variance = mzabs * mzabs;
                double matchScore = scorePeaks(lp, rp, variance);
                score += matchScore;
                for (int k = i + 1; k < nl; ++k) {
                    DataPoint lp2 = left[k];
                    final double difference2 = lp2.getMZ() - rp.getMZ();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp2, rp, variance);
                        score += matchScore;
                    } else break;
                }
                for (int l = j + 1; l < nr; ++l) {
                    DataPoint rp2 = right[l];
                    final double difference2 = lp.getMZ() - rp2.getMZ();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp, rp2, variance);
                        score += matchScore;
                    } else break;
                }
                ++i;
                ++j;
            } else if (difference > 0) {
                ++j;

            } else {
                ++i;
            }
        }
        return score;

    }

    /**
     * Internal method for calculating the gaussian integral product
     */
    protected static double scorePeaks(DataPoint lp, DataPoint rp, double variance) {
        //formula from Jebara: Probability Product Kernels. multiplied by intensities
        // (1/(4*pi*sigma**2))*exp(-(mu1-mu2)**2/(4*sigma**2))
        final double mzDiff = Math.abs(lp.getMZ() - rp.getMZ());
        final double constTerm = 1.0 / (Math.PI * variance * 4);

        final double propOverlap = constTerm * Math.exp(-(mzDiff * mzDiff) / (4 * variance));
        return (lp.getIntensity() * rp.getIntensity()) * propOverlap;
    }

    protected static final Comparator<DataPoint> CompareDataPointsByMz = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return Double.compare(o1.getMZ(), o2.getMZ());
        }
    };
    protected static final Comparator<DataPoint> CompareDataPointsByDecreasingInt = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return Double.compare(o2.getIntensity(), o1.getIntensity());
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * An MS/MS scan with some statistics about its precursor in MS
     */
    protected static class FragmentScan {

        /**
         * The raw data file this scans are derived from
         */
        protected final RawDataFile origin;
        /**
         * the MS1 scan that comes before the first MS/MS
         */
        protected final int ms1ScanNumber;
        /**
         * all consecutive(!) MS/MS scans. There should ne no other MS1 scan between them
         */
        protected final int[] ms2ScanNumbers;
        /**
         * the intensity of the precursor peak in MS
         */
        protected double precursorIntensity;
        /**
         * the sumed up intensity of chimeric peaks
         */
        protected double chimericIntensity;

        public FragmentScan(RawDataFile origin, double precursorMass, int ms1ScanNumber, int[] ms2ScanNumbers, Range<Double> isolationWindow, double massAccuracyInPPM) {
            this.origin = origin;
            this.ms1ScanNumber = ms1ScanNumber;
            this.ms2ScanNumbers = ms2ScanNumbers;
            if (ms1ScanNumber >= 0) {
                detectPrecursor(precursorMass, isolationWindow, massAccuracyInPPM);
            } else {
                this.precursorIntensity = 0d;
                this.chimericIntensity = 0d;
            }
        }

        /**
         * the score describes how good this MS/MS are. Should incorporate the intensity of MS1 precursor peak
         * and the intensity of closeby chimeric peaks
         */
        private double getScore() {
            return precursorIntensity * (precursorIntensity/getChimericIntensityWithPseudocount());
        }

        protected double getChimericIntensityWithPseudocount() {
            return (chimericIntensity+precursorIntensity*CHIMERIC_INTENSITY_THRESHOLD);
        }

        /**
         * search for precursor peak in MS1
         */
        private void detectPrecursor(double precursorMass, Range<Double> isolationWindow, double ppm) {
            Scan spectrum = origin.getScan(ms1ScanNumber);
            DataPoint[] dps = spectrum.getDataPointsByMass(Range.closed(precursorMass+isolationWindow.lowerEndpoint(), precursorMass+isolationWindow.upperEndpoint()));
            // for simplicity, just use the most intensive peak within ppm range
            int bestPeak = -1;
            double highestIntensity = 0d;
            for (int mppm = 1; mppm < 3; ++mppm) {
                final double maxDiff = (mppm * ppm) * 1e-6 * Math.max(200, precursorMass);
                for (int i = 0; i < dps.length; ++i) {
                    final DataPoint p = dps[i];
                    if (p.getIntensity() <= highestIntensity)
                        continue;
                    final double mzdiff = Math.abs(p.getMZ() - precursorMass);
                    if (mzdiff <= maxDiff) {
                        highestIntensity = p.getIntensity();
                        bestPeak = i;
                    }
                }
                if (bestPeak >= 0)
                    break;
            }
            // now sum up all remaining intensities. Leave out isotopes. leave out peaks with intensity below 10%
            // of the precursor. They won't contaminate fragment scans anyways
            this.precursorIntensity = highestIntensity;
            this.chimericIntensity = 0d;
            final double threshold = precursorIntensity* CHIMERIC_INTENSITY_THRESHOLD;
            foreachpeak:
            for (int i = 0; i < dps.length; ++i) {
                if (i != bestPeak && dps[i].getIntensity()>threshold) {
                    // check for isotope peak
                    final double maxDiff = ppm * 1e-6 * Math.max(200, precursorMass) + 0.03;
                    for (int k = 1; k < 5; ++k) {
                        final double isoMz = precursorMass + k * 1.0015;
                        final double diff = isoMz - dps[i].getMZ();
                        if (Math.abs(diff) <= maxDiff) {
                            continue foreachpeak;
                        } else if (diff > 0.5) {
                            break;
                        }
                    }
                    chimericIntensity += dps[i].getIntensity();
                }
            }
        }
    }

    protected static class MergedSpectrum {
        protected final MergeDataPoint[] data;
        protected final RawDataFile[] origins;
        protected final int[] scanIds;
        protected int removedScansByLowQuality;
        protected int removedScansByLowCosine;
        private final static MergedSpectrum EMPTY = new MergedSpectrum(new MergeDataPoint[0],new RawDataFile[0],new int[0], 0,0);
        public static MergedSpectrum empty() {
            return EMPTY;
        }

        public MergedSpectrum(MergeDataPoint[] data, Set<RawDataFile> origins, Set<Integer> scanIds, int removedScansByLowQuality, int removedScansByLowCosine) {
            this(data, origins.toArray(new RawDataFile[origins.size()]), scanIds.stream().sorted().mapToInt(x->x).toArray(), removedScansByLowQuality, removedScansByLowCosine);
        }

        public MergedSpectrum(MergeDataPoint[] data, RawDataFile[] origins, int[] scanIds, int removedScansByLowQuality, int removedScansByLowCosine) {
            this.data = data;
            this.origins = origins;
            this.scanIds = scanIds;
            this.removedScansByLowQuality = removedScansByLowQuality;
            this.removedScansByLowCosine = removedScansByLowCosine;
        }

        public String toString() {
            if (origins.length>0) {
                final String name = origins[0].getName();
                if (origins.length>1) {
                    return "Merged spectrum from " + name + " and " + (origins.length-1) + " others";
                } else {
                    return "Merged spectrum from " + name;
                }
            } else return "merged spectrum";
        }

        public int totalNumberOfScans() {
            return scanIds.length+removedScansByLowCosine+removedScansByLowQuality;
        }

        public MergedSpectrum merge(MergedSpectrum right, MergeDataPoint[] mergedSpectrum) {
            final RawDataFile[] norigs = Arrays.copyOf(origins, origins.length+right.origins.length);
            System.arraycopy(right.origins, 0, norigs, origins.length, right.origins.length);
            final int[] nscans = Arrays.copyOf(scanIds, scanIds.length+right.scanIds.length);
            System.arraycopy(right.scanIds, 0, nscans, scanIds.length, right.scanIds.length);
            return new MergedSpectrum(mergedSpectrum, norigs, nscans, removedScansByLowQuality+right.removedScansByLowQuality, removedScansByLowCosine+right.removedScansByLowCosine);
        }

        public double getTIC() {
            double tic = 0d;
            for (MergeDataPoint p : data)
                tic += p.maxInt;
            return tic;
        }

        public MergedSpectrum filterByNumberOfScans(int minimumNumberOfScans) {
            return new MergedSpectrum(
                    Arrays.stream(data).filter(x->x.sources.length >= minimumNumberOfScans).toArray(MergeDataPoint[]::new),
                    origins,
                    scanIds,
                    removedScansByLowQuality,
                    removedScansByLowCosine
            );
        }
    }

    protected static class MergeDataPoint implements DataPoint {

        private final DataPoint[] sources;
        private final double mz, sumIntensity;

        private final double sumMz, maxInt;

        private MergeDataPoint(DataPoint[] sources, double mz, double intens, double sumMz, double maxIntens) {
            this.sources = sources;
            this.mz = mz;
            this.sumIntensity = intens;
            this.sumMz = sumMz;
            this.maxInt = maxIntens;
        }

        private MergeDataPoint(DataPoint single) {
            this.sources = new DataPoint[]{single};
            this.mz = single.getMZ();
            this.sumIntensity = single.getIntensity();
            this.sumMz = this.mz * this.sumIntensity;
            this.maxInt = single.getIntensity();
        }

        public String toString() {
            return String.valueOf(mz) + " " + String.valueOf(sumIntensity);
        }

        /**
         * Sort peaks by m/z, remove the 25% quantile from both sides. Recalculate average m/z again
         */
        protected MergeDataPoint cutoffOutliers() {
            DataPoint[] sources = this.sources.clone();
            Arrays.sort(sources, CompareDataPointsByMz);
            int quantil = (int)Math.floor(sources.length*0.25);
            double m = 0d, i = 0d;
            for (int k=quantil, n = (sources.length-quantil); k < n; ++k) {
                m += sources[k].getMZ()*sources[k].getIntensity();
                i += sources[k].getIntensity();
            }
            m /= i;
            return new MergeDataPoint(sources, m, this.sumIntensity, this.sumMz, this.maxInt );
        }

        protected MergeDataPoint merge(DataPoint additional, boolean mergeMz) {
            if (additional instanceof MergeDataPoint) {
                final MergeDataPoint ad = (MergeDataPoint)additional;
                DataPoint[] cop = Arrays.copyOf(sources, sources.length + ad.sources.length);
                System.arraycopy(ad.sources, 0, cop, sources.length, ad.sources.length );
                final double sumMz2 = sumMz + additional.getMZ() * additional.getIntensity();
                final double sumInt = sumIntensity + additional.getIntensity();
                return new MergeDataPoint(cop, mergeMz ? sumMz2 / sumInt : (additional.getIntensity() > maxInt ? additional.getMZ() : mz), sumInt, sumMz2, Math.max(maxInt, additional.getIntensity()));
            } else {
                DataPoint[] cop = Arrays.copyOf(sources, sources.length + 1);
                cop[cop.length - 1] = additional;
                final double sumMz2 = sumMz + additional.getMZ() * additional.getIntensity();
                final double sumInt = sumIntensity + additional.getIntensity();
                return new MergeDataPoint(cop, mergeMz ? sumMz2 / sumInt : (additional.getIntensity() > maxInt ? additional.getMZ() : mz), sumInt, sumMz2, Math.max(maxInt, additional.getIntensity()));
            }
        }

        public String getComment() {
            double smallest = Double.POSITIVE_INFINITY, largest = Double.NEGATIVE_INFINITY, average = 0d;
            int apex = 0;
            for (int k = 0; k < sources.length; ++k) {
                final DataPoint p = sources[k];
                smallest = Math.min(smallest, p.getMZ());
                largest = Math.max(largest, p.getMZ());
                average += Math.abs(p.getMZ() - mz);
                if (p.getIntensity() > sources[apex].getIntensity()) apex = k;
            }
            average /= sources.length;
            // median
            final DataPoint[] copy = sources.clone();
            Arrays.sort(copy, CompareDataPointsByMz);
            double medianMz = copy[copy.length / 2].getMZ();
            if (copy.length > 1 && copy.length % 2 == 0)
                medianMz = (medianMz + copy[(copy.length) / 2 - 1].getMZ()) / 2d;
            return String.format(Locale.US, "%.5f ... %.5f  (median = %.5f, apex = %.5f). Standard deviation = %.5f. Peaks: %d", smallest, largest, medianMz, sources[apex].getMZ(), average, sources.length);
        }

        @Override
        public double getMZ() {
            return mz;
        }

        @Override
        public double getIntensity() {
            return maxInt;
        }

        public double getSumIntensity() {
            return sumIntensity;
        }

    }

    protected static class CosineSpectrum {
        protected final DataPoint[] dataPoints;
        protected final double norm;
        protected final double expectedPPM, precursorMz;

        public CosineSpectrum(DataPoint[] dataPoints, double precursorMz, double expectedPPM) {
            final ArrayList<DataPoint> dps = new ArrayList<>();
            // in each 100 Da intervall, only keep the 6 most intensive peaks
            final List<List<DataPoint>> chunks = new ArrayList<>();
            for (DataPoint dp : dataPoints) {
                int bin = (int)Math.ceil(dp.getMZ()/100d);
                while (chunks.size()<=bin) chunks.add(new ArrayList<>());
                chunks.get(bin).add(dp);
            }
            for (List<DataPoint> chunk : chunks) {
                chunk.sort(Comparator.comparingDouble(x->-x.getIntensity()));
                for (int k=0; k < Math.min(6,chunk.size()); ++k) {
                    dps.add(chunk.get(k));
                }
            }
            dps.sort(Comparator.comparingDouble(x->x.getMZ()));
            this.dataPoints = dps.toArray(new DataPoint[dps.size()]);
            this.norm = probabilityProduct(this.dataPoints,this.dataPoints, expectedPPM, precursorMz-17, 0d);
            this.expectedPPM = expectedPPM;
            this.precursorMz = precursorMz;
        }

        public double cosine(CosineSpectrum other) {
            return probabilityProduct(dataPoints, other.dataPoints,expectedPPM, (precursorMz+other.precursorMz)/2d, 0) /Math.sqrt(norm*other.norm);
        }
    }

    public static class MergedStatistics {

        protected final TDoubleArrayList tics;
        protected final AtomicInteger numberOfHighResCosineFails=new AtomicInteger(0);
        protected final AtomicInteger numberOfLowCosines=new AtomicInteger(0);
        protected double percentile10TIC =Double.NaN;
        protected int lastTimeMedianCalc=0;
        protected final ArrayList<DelayedWarning> warnings;
        protected final TDoubleArrayList examplesOfNoisePeaks = new TDoubleArrayList();

        public MergedStatistics() {
            tics = new TDoubleArrayList();
            this.warnings = new ArrayList<>();
        }

        protected double get10PercentileTIC() {
            calculateMedian();
            return this.percentile10TIC;
        }

        protected synchronized void warnForLowCosine(String warningMessage, double tic) {
            final double median = calculateMedian();
            if (Double.isInfinite(median)) {
                flushWarningQueue();
                if (tic >= percentile10TIC) {
                    LoggerFactory.getLogger(MergeUtils.class).warn(warningMessage);
                }
            } else {
                warnings.add(new DelayedWarning(warningMessage, tic));
            }
        }

        protected synchronized void flushWarningQueue() {
            calculateMedian();
            for (DelayedWarning w : warnings) {
                if (w.tic >= percentile10TIC) {
                    LoggerFactory.getLogger(MergeUtils.class).warn(w.warningMessage);
                }
            }
            warnings.clear();
        }

        private synchronized double calculateMedian() {
            if (tics.size()<100) return Double.NaN;
            if ((tics.size()/((double)lastTimeMedianCalc)) >= 2) {
                tics.sort();
                this.percentile10TIC = tics.getQuick((int)(tics.size()*0.1));
                this.lastTimeMedianCalc = tics.size();
            }
            return percentile10TIC;
        }

        public int getNumberOfHighResCosineFails() {
            return numberOfHighResCosineFails.get();
        }

        public int getNumberOfLowCosines() {
            return numberOfLowCosines.get();
        }

        protected synchronized void addTic(double tic) {
            tics.add(tic);
        }

        protected synchronized void incrementLowCosine() {
            numberOfLowCosines.incrementAndGet();
        }

        protected void incrementLowResHighResDifference() {
            numberOfHighResCosineFails.incrementAndGet();
        }
    }

    protected static class DelayedWarning {
        private final String warningMessage;
        private final double tic;
        protected DelayedWarning(String warningMessage, double tic) {
            this.warningMessage = warningMessage;
            this.tic = tic;
        }
    }

}
