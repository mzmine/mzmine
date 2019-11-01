package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import net.sf.mzmine.datamodel.PeakListRow;

public class MatchResult {
  
  private final PeakListRow bestRow;
  private final double score;
  
  public MatchResult(PeakListRow bestRow, double score) {
    this.bestRow = bestRow;
    this.score = score;
  }

  public PeakListRow getBestRow() {
    return bestRow;
  }

  public double getScore() {
    return score;
  }
  
}
