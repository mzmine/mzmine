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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.path.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.PathAlignerParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.scorer.RTScore;
import net.sf.mzmine.parameters.ParameterSet;

public class ScoreAligner implements Aligner {

    public final static String name = "Aligner";
    private int peaksTotal;
    private int peaksDone;
    private volatile boolean aligningDone;
    private volatile Thread[] threads;
    private volatile List<List<PeakListRow>> peakList;
    private final List<PeakList> originalPeakList;
    private ScoreCalculator calc;
    private PeakList alignment;
    private ParameterSet params;

    public ScoreAligner(PeakList[] dataToAlign, ParameterSet params) {
	this.params = params;
	this.calc = new RTScore();
	originalPeakList = java.util.Collections.unmodifiableList(Arrays
		.asList(dataToAlign));
	copyAndSort(Arrays.asList(dataToAlign));
    }

    private void copyAndSort(List<PeakList> dataToAlign) {
	Comparator<PeakList> c = new Comparator<PeakList>() {

	    public int compare(PeakList o1, PeakList o2) {
		return o2.getNumberOfRows() - o1.getNumberOfRows();
	    }
	};

	if (dataToAlign != null) {
	    List<PeakList> copyOfData = new ArrayList<PeakList>(dataToAlign);
	    java.util.Collections.sort(copyOfData, c);
	    peakList = new ArrayList<List<PeakListRow>>();
	    for (int i = 0; i < copyOfData.size(); i++) {
		PeakListRow[] peakData = copyOfData.get(i).getRows();
		List<PeakListRow> peaksInOneFile = new LinkedList<PeakListRow>();
		peaksInOneFile.addAll(Arrays.asList(peakData));
		peakList.add(peaksInOneFile);
	    }
	} else {
	    peakList = null;
	}
    }

    private List<AlignmentPath> generatePathsThreaded(final ScoreCalculator c,
	    final List<List<PeakListRow>> peaksToUse)
	    throws CancellationException {
	final List<AlignmentPath> paths = Collections
		.synchronizedList(new LinkedList<AlignmentPath>());
	final List<AlignmentPath> completePaths = new ArrayList<AlignmentPath>();
	final int numThreads = Runtime.getRuntime().availableProcessors();
	final AlignerThread aligners[] = new AlignerThread[numThreads];

	Runnable barrierTask = new Runnable() {

	    public void run() {
		Collections.sort(paths);
		while (paths.size() > 0) {
		    Iterator<AlignmentPath> iter = paths.iterator();
		    AlignmentPath best = iter.next();
		    iter.remove();
		    while (iter.hasNext()) {
			AlignmentPath cand = iter.next();
			if (best.containsSame(cand)) {
			    iter.remove();
			}
		    }
		    completePaths.add(best);
		    removePeaks(best, peaksToUse);
		    peaksDone += best.nonEmptyPeaks();
		}

		// Empty the list for further use
		paths.clear();
		int currentCol = -1;
		for (int i = 0; i < peaksToUse.size(); i++) {
		    if (peaksToUse.get(i).size() > 0) {
			currentCol = i;
			break;
		    }
		}
		if (currentCol == -1) {
		    aligningDone = true;
		    return;
		}

		ThreadInfo threadInfos[] = calculateIntervals(numThreads,
			currentCol, peaksToUse);
		for (int i = 0; i < numThreads; i++) {
		    aligners[i].setThreadInfo(threadInfos[i]);
		}
	    }
	};

	CyclicBarrier barrier = new CyclicBarrier(numThreads, barrierTask);

	// Preliminary setup of thread working
	{
	    int currentCol = 0;
	    ThreadInfo threadInfos[] = calculateIntervals(numThreads,
		    currentCol, peaksToUse);
	    for (int i = 0; i < numThreads; i++) {
		aligners[i] = new AlignerThread(threadInfos[i], barrier, paths,
			c, peaksToUse);
	    }
	}

	threads = new Thread[aligners.length];
	for (int i = 0; i < aligners.length; i++) {
	    threads[i] = (new Thread(aligners[i]));
	    threads[i].start();
	}
	for (int i = 0; i < aligners.length; i++) {
	    try {
		threads[i].join();
	    } catch (InterruptedException e) {
		break;
		// TODO Add perhaps more resilence to unforeseen turns of
		// events.
		// At least now there should not be any case when this main
		// thread
		// would be interrupted.
	    }
	}
	return completePaths;
    }

    private AlignmentPath generatePath(int col, ScoreCalculator c,
	    PeakListRow base, List<List<PeakListRow>> listOfPeaksInFiles) {
	int len = listOfPeaksInFiles.size();
	AlignmentPath path = new AlignmentPath(len, base, col);
	for (int i = (col + 1) % len; i != col; i = (i + 1) % len) {

	    PeakListRow bestPeak = null;
	    double bestPeakScore = c.getWorstScore();
	    for (PeakListRow curPeak : listOfPeaksInFiles.get(i)) {
		if (curPeak == null || !c.matches(path, curPeak, params)) {
		    // Either there isn't any peak left or it doesn't fill
		    // requirements of current score calculator (for example,
		    // it doesn't have a name).
		    continue;
		}
		double score = c.calculateScore(path, curPeak, params);

		if (score < bestPeakScore) {
		    bestPeak = curPeak;
		    bestPeakScore = score;
		}

	    }

	    double gapPenalty = 1.25;

	    if (bestPeak != null && bestPeakScore < gapPenalty) {
		path.add(i, bestPeak, bestPeakScore);
	    } else {
		path.addGap(i, gapPenalty);
	    }

	}
	return path;
    }

    private void removePeaks(AlignmentPath p,
	    List<List<PeakListRow>> listOfPeaks) {
	for (int i = 0; i < p.length(); i++) {
	    PeakListRow d = p.getPeak(i);
	    if (d != null) {
		listOfPeaks.get(i).remove(d);
	    }
	}
    }

    private boolean aligningDone() {
	return aligningDone;
    }

    private ThreadInfo[] calculateIntervals(int threads, int col,
	    List<List<PeakListRow>> listOfPeaks) {
	int diff = listOfPeaks.get(col).size() / threads;
	ThreadInfo threadInfos[] = new ThreadInfo[threads];
	for (int i = 0; i < threads; i++) {
	    threadInfos[i] = new ThreadInfo(i * diff,
		    ((i == threads - 1) ? listOfPeaks.get(col).size() : (i + 1)
			    * diff), col);
	}
	return threadInfos;
    }

    public double getProgress() {
	return ((double) this.peaksDone / (double) this.peaksTotal);
    }

    private List<AlignmentPath> getAlignmentPaths()
	    throws CancellationException {
	List<AlignmentPath> paths = new ArrayList<AlignmentPath>();
	paths = generatePathsThreaded(calc, peakList);
	return paths;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gcgcaligner.AbstractAligner#align()
     */
    public PeakList align() {

	if (alignment == null) // Do the actual alignment if we already do not
			       // have the result
	{
	    Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
	    for (PeakList list : this.originalPeakList) {
		allDataFiles.addAll(Arrays.asList(list.getRawDataFiles()));
	    }

	    peaksTotal = 0;
	    for (int i = 0; i < peakList.size(); i++) {
		peaksTotal += peakList.get(i).size();
	    }
	    alignment = new SimplePeakList(params.getParameter(
		    PathAlignerParameters.peakListName).getValue(),
		    allDataFiles.toArray(new RawDataFile[0]));

	    List<AlignmentPath> addedPaths = getAlignmentPaths();
	    int ID = 1;
	    for (AlignmentPath p : addedPaths) {
		// Convert alignments to original order of files and add them to
		// final
		// Alignment data structure
		PeakListRow row = (PeakListRow) p.convertToAlignmentRow(ID++);
		alignment.addRow(row);

	    }
	}

	PeakList curAlignment = alignment;
	return curAlignment;
    }

    public String toString() {
	return getName();
    }

    public String getName() {
	return calc == null ? name : calc.name();
    }

    public ParameterSet getParameters() {
	return params;
    }

    private class AlignerThread implements Runnable {

	private ThreadInfo ti;
	private CyclicBarrier barrier;
	private List<AlignmentPath> readyPaths;
	private ScoreCalculator calc;
	private List<List<PeakListRow>> listOfAllPeaks;

	public void setThreadInfo(ThreadInfo ti) {
	    this.ti = ti;
	}

	public AlignerThread(ThreadInfo ti, CyclicBarrier barrier,
		List<AlignmentPath> readyPaths, ScoreCalculator c,
		List<List<PeakListRow>> peakList) {
	    this.ti = ti;
	    this.barrier = barrier;
	    this.readyPaths = readyPaths;
	    this.calc = c;
	    this.listOfAllPeaks = peakList;
	}

	private void align() {
	    List<PeakListRow> myList = listOfAllPeaks.get(ti.currentColumn())
		    .subList(ti.startIx(), ti.endIx());
	    Queue<AlignmentPath> myPaths = new LinkedList<AlignmentPath>();
	    for (PeakListRow cur : myList) {
		if (cur != null) {
		    AlignmentPath p = generatePath(ti.currentColumn(), calc,
			    cur, listOfAllPeaks);
		    if (p != null) {
			myPaths.offer(p);
		    }
		}
	    }
	    readyPaths.addAll(myPaths);
	}

	public void run() {
	    while (!aligningDone()) {
		align();
		// Exceptions cause wrong results but do not report
		// that in any way
		try {
		    barrier.await();
		} catch (InterruptedException e) {
		    return;
		} catch (BrokenBarrierException e2) {
		    return;
		} catch (CancellationException e3) {
		    return;
		}
	    }
	}
    }

    private static class ThreadInfo {

	/**
	 * Start index is inclusive, end index exclusive.
	 */
	private int startIx;
	private int endIx;
	private int column;

	public ThreadInfo(int startIx, int endIx, int col) {
	    this.startIx = startIx;
	    this.endIx = endIx;
	    this.column = col;
	}

	public int currentColumn() {
	    return column;
	}

	public int endIx() {
	    return endIx;
	}

	public int startIx() {
	    return startIx;
	}

    }

    public boolean isConfigurable() {
	return true;
    }

    protected void resetThings() {
	copyAndSort(originalPeakList);
	alignment = null;
    }

    protected void doCancellingActions() {
	for (Thread th : threads) {
	    if (th != null) {
		th.interrupt();
	    }
	}

    }
}
