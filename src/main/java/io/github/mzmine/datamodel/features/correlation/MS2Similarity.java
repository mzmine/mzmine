package io.github.mzmine.datamodel.features.correlation;

public class MS2Similarity {
  private double cosine;
  private int overlap;

  public MS2Similarity(double cosine, int overlap) {
    super();
    this.cosine = cosine;
    this.overlap = overlap;
  }

  /**
   * Number of overlapping (matching) signals
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
