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
package io.github.mzmine.modules.dataprocessing.align_path.functions;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.align_path.PathAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_path.scorer.RTScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
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
import org.jetbrains.annotations.Nullable;

public class ScoreAligner implements Aligner {

  public final static String name = "Aligner";
  private int peaksTotal;
  private int peaksDone;
  private volatile boolean aligningDone;
  private volatile Thread[] threads;
  private volatile List<List<FeatureListRow>> peakList;
  private final List<FeatureList> originalPeakList;
  private ScoreCalculator calc;
  private FeatureList alignment;
  private ParameterSet params;
  private final MemoryMapStorage storage;

  public ScoreAligner(FeatureList[] dataToAlign, ParameterSet params, @Nullable MemoryMapStorage storage) {
    this.storage = storage;
    this.params = params;
    this.calc = new RTScore();
    originalPeakList = java.util.Collections.unmodifiableList(Arrays.asList(dataToAlign));
    copyAndSort(Arrays.asList(dataToAlign));
  }

  private void copyAndSort(List<FeatureList> dataToAlign) {
    Comparator<FeatureList> c = new Comparator<FeatureList>() {

      @Override
      public int compare(FeatureList o1, FeatureList o2) {
        return o2.getNumberOfRows() - o1.getNumberOfRows();
      }
    };

    if (dataToAlign != null) {
      List<FeatureList> copyOfData = new ArrayList<FeatureList>(dataToAlign);
      java.util.Collections.sort(copyOfData, c);
      peakList = new ArrayList<List<FeatureListRow>>();
      for (int i = 0; i < copyOfData.size(); i++) {
        FeatureListRow[] peakData = copyOfData.get(i).getRows().toArray(FeatureListRow[]::new);
        List<FeatureListRow> peaksInOneFile = new LinkedList<FeatureListRow>();
        peaksInOneFile.addAll(Arrays.asList(peakData));
        peakList.add(peaksInOneFile);
      }
    } else {
      peakList = null;
    }
  }

  private List<AlignmentPath> generatePathsThreaded(final ScoreCalculator c,
      final List<List<FeatureListRow>> peaksToUse) throws CancellationException {
    final List<AlignmentPath> paths = Collections.synchronizedList(new LinkedList<AlignmentPath>());
    final List<AlignmentPath> completePaths = new ArrayList<AlignmentPath>();
    final int numThreads = Runtime.getRuntime().availableProcessors();
    final AlignerThread aligners[] = new AlignerThread[numThreads];

    Runnable barrierTask = new Runnable() {

      @Override
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

        ThreadInfo threadInfos[] = calculateIntervals(numThreads, currentCol, peaksToUse);
        for (int i = 0; i < numThreads; i++) {
          aligners[i].setThreadInfo(threadInfos[i]);
        }
      }
    };

    CyclicBarrier barrier = new CyclicBarrier(numThreads, barrierTask);

    // Preliminary setup of thread working
    {
      int currentCol = 0;
      ThreadInfo threadInfos[] = calculateIntervals(numThreads, currentCol, peaksToUse);
      for (int i = 0; i < numThreads; i++) {
        aligners[i] = new AlignerThread(threadInfos[i], barrier, paths, c, peaksToUse);
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

  private AlignmentPath generatePath(int col, ScoreCalculator c, FeatureListRow base,
      List<List<FeatureListRow>> listOfPeaksInFiles) {
    int len = listOfPeaksInFiles.size();
    AlignmentPath path = new AlignmentPath(len, base, col);
    for (int i = (col + 1) % len; i != col; i = (i + 1) % len) {

      FeatureListRow bestPeak = null;
      double bestPeakScore = c.getWorstScore();
      for (FeatureListRow curPeak : listOfPeaksInFiles.get(i)) {
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

  private void removePeaks(AlignmentPath p, List<List<FeatureListRow>> listOfPeaks) {
    for (int i = 0; i < p.length(); i++) {
      FeatureListRow d = p.getPeak(i);
      if (d != null) {
        listOfPeaks.get(i).remove(d);
      }
    }
  }

  private boolean aligningDone() {
    return aligningDone;
  }

  private ThreadInfo[] calculateIntervals(int threads, int col,
      List<List<FeatureListRow>> listOfPeaks) {
    int diff = listOfPeaks.get(col).size() / threads;
    ThreadInfo threadInfos[] = new ThreadInfo[threads];
    for (int i = 0; i < threads; i++) {
      threadInfos[i] = new ThreadInfo(i * diff,
          ((i == threads - 1) ? listOfPeaks.get(col).size() : (i + 1) * diff), col);
    }
    return threadInfos;
  }

  @Override
  public double getProgress() {
    return ((double) this.peaksDone / (double) this.peaksTotal);
  }

  private List<AlignmentPath> getAlignmentPaths() throws CancellationException {
    List<AlignmentPath> paths = new ArrayList<AlignmentPath>();
    paths = generatePathsThreaded(calc, peakList);
    return paths;
  }

  /*
   * (non-Javadoc)
   *
   * @see gcgcaligner.AbstractAligner#align()
   */
  @Override
  public FeatureList align() {

    if (alignment == null) // Do the actual alignment if we already do not
    // have the result
    {
      Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
      for (FeatureList list : this.originalPeakList) {
        allDataFiles.addAll(list.getRawDataFiles());
      }

      peaksTotal = 0;
      for (int i = 0; i < peakList.size(); i++) {
        peaksTotal += peakList.get(i).size();
      }
      alignment =
          new ModularFeatureList(params.getParameter(PathAlignerParameters.peakListName).getValue(), storage,
              allDataFiles.toArray(new RawDataFile[0]));

      List<AlignmentPath> addedPaths = getAlignmentPaths();
      int ID = 1;
      for (AlignmentPath p : addedPaths) {
        // Convert alignments to original order of files and add them to
        // final
        // Alignment data structure
        FeatureListRow row = p.convertToAlignmentRow(ID++);
        alignment.addRow(row);

      }
    }

    FeatureList curAlignment = alignment;
    return curAlignment;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
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
    private List<List<FeatureListRow>> listOfAllPeaks;

    public void setThreadInfo(ThreadInfo ti) {
      this.ti = ti;
    }

    public AlignerThread(ThreadInfo ti, CyclicBarrier barrier, List<AlignmentPath> readyPaths,
        ScoreCalculator c, List<List<FeatureListRow>> peakList) {
      this.ti = ti;
      this.barrier = barrier;
      this.readyPaths = readyPaths;
      this.calc = c;
      this.listOfAllPeaks = peakList;
    }

    private void align() {
      List<FeatureListRow> myList =
          listOfAllPeaks.get(ti.currentColumn()).subList(ti.startIx(), ti.endIx());
      Queue<AlignmentPath> myPaths = new LinkedList<AlignmentPath>();
      for (FeatureListRow cur : myList) {
        if (cur != null) {
          AlignmentPath p = generatePath(ti.currentColumn(), calc, cur, listOfAllPeaks);
          if (p != null) {
            myPaths.offer(p);
          }
        }
      }
      readyPaths.addAll(myPaths);
    }

    @Override
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
