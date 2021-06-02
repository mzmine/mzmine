package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * correlation of two feature shapes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CorrelationData {

  // data points
  // [feature a, b][data point intensity]
  private final double[][] data;
  private final SimpleRegression reg;
  private final double minX;
  private final double maxX;

  // cosineSimilarity
  private final double cosineSim = 0;

  public CorrelationData(double[][] data) {
    this.data = data;
  }

  /**
   * Extracts all data from all correlations
   *
   * @param corr
   */
  public static CorrelationData create(Collection<CorrelationData> corr) {
    List<double[]> dat = new ArrayList<>();
    for (CorrelationData c : corr) {
      for (double[] d : c.getData()) {
        dat.add(d);
      }
    }
    return create(dat);
  }

  public static CorrelationData create(List<double[]> dat) {
    CorrelationData c = new CorrelationData();
    c.reg = new SimpleRegression();
    c.data = new double[dat.size()][2];
    c.minX = Double.POSITIVE_INFINITY;
    c.maxX = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < dat.size(); i++) {
      c.data[i][0] = dat.get(i)[0];
      c.data[i][1] = dat.get(i)[1];
      c.minX = Math.min(c.minX, c.data[i][0]);
      c.maxX = Math.max(c.maxX, c.data[i][0]);
    }
    c.reg.addData(c.data);
    // calc cosineSim
    c.cosineSim = Similarity.COSINE.calc(c.data);
    return c;
  }

  public SimpleRegression getReg() {
    return reg;
  }

  public void setReg(SimpleRegression reg) {
    this.reg = reg;
  }

  public int getDPCount() {
    return reg == null ? 0 : (int) reg.getN();
  }

  /**
   * Pearson correlation
   *
   * @return
   */
  public double getR() {
    return reg == null ? 0 : reg.getR();
  }

  /**
   * Cosine similarity
   *
   * @return
   */
  public double getCosineSimilarity() {
    return cosineSim;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  public double getSimilarity(SimilarityMeasure type) {
    if (data == null || data.length == 0) {
      return Double.NaN;
    } else {
      return type.calc(data);
    }
  }

  public double getMinX() {
    return minX;
  }

  public void setMinX(double minX) {
    this.minX = minX;
  }

  public double getMaxX() {
    return maxX;
  }

  public void setMaxX(double maxX) {
    this.maxX = maxX;
  }

  public double[][] getData() {
    return data;
  }

  public boolean isValid() {
    return getDPCount() > 0;
  }

  /**
   * @return X (intensity of row)
   */
  public double getX(int i) {
    return data == null ? 0 : data[i][0];
  }

  /**
   * @return Y (intensity of compared row)
   */
  public double getY(int i) {
    return data == null ? 0 : data[i][1];
  }
}
