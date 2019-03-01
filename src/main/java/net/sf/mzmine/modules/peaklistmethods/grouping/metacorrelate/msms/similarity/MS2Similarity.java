package net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity;

public class MS2Similarity {
  private double cosine;
  private int overlap;

  public MS2Similarity(double cosine, int overlap) {
    super();
    this.cosine = cosine;
    this.overlap = overlap;
  }

  /**
   * Number of overlapping signals
   * 
   * @return
   */
  public int getOverlap() {
    return overlap;
  }

  /**
   * Cosine similarity
   * 
   * @return
   */
  public double getCosine() {
    return cosine;
  }

}
