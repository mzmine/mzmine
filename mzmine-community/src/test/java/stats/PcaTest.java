/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package stats;


import io.github.mzmine.modules.dataanalysis.pca_new.PCAResult;
import io.github.mzmine.modules.dataanalysis.pca_new.PCAUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;

public class PcaTest {

  private static final Logger logger = Logger.getLogger(PcaTest.class.getName());
  private static final String datastr = """
      3	8	2	3	4	5	2	9	0	2	8	2
      3	8	3	2	4	5	2	8	0	2	9	2
      3	8	4	5	4	5	2	8	0	2	7	2
      3	8	3	2	4	5	3	8	2	2	9	3
      3	8	4	2	3	5	3	8	0	2	8	3
      9	1	2	2	3	5	1	8	3	2	8	1
      9	1	1	5	2	5	2	9	2	2	9	2
      9	1	2	2	3	5	4	9	2	2	7	2
      9	1	3	5	3	5	3	9	1	2	8	3
      9	1	3	2	4	5	2	8	1	2	9	3
      """;

  private static String[] rows = datastr.split("\n");

  @Test
  void pcaTest() throws InterruptedException {
//    double[][] data = new double[][]{{5.1, 3.5, 1.4, 0.2, 1}, {4.9, 3.0, 1.4, 0.2, 1},
//        {4.7, 3.2, 1.3, 0.2, 1}, {4.6, 3.1, 1.5, 0.2, 1}, {5.0, 3.6, 1.4, 0.2, 1}};
    final double[][] data = Arrays.stream(rows).map(str -> str.split("\\s+"))
        .map(strings -> Arrays.stream(strings).mapToDouble(Double::valueOf).toArray())
        .toArray(double[][]::new);

    RealMatrix matrix = new Array2DRowRealMatrix(data);

    final RealMatrix centered = PCAUtils.performMeanCenter(matrix, false);
    logger.info(centered.toString());

    final PCAResult pcaResult = PCAUtils.calculatePCA(matrix);

    final RealMatrix principalComponentMatrix = pcaResult.principalComponentsMatrix();
    final RealMatrix first2Components = pcaResult.firstNComponents(2);

//    final RealMatrix projected = matrix.multiply(first2Components);
    final RealMatrix projected = pcaResult.projectDataToScores(2);

    final ScoresPlot scoresPlot = new ScoresPlot(projected.getData());
    scoresPlot.setVisible(true);
    JDialog frame = new JDialog();
    frame.setContentPane(scoresPlot);
    frame.setVisible(true);

    logger.info(() -> STR."Scores: \{principalComponentMatrix.toString()}");
    logger.info(() -> STR."Loadings: \{pcaResult.getLoadingsMatrix().toString()}");
  }

  class ScoresPlot extends JPanel {

    private double[][] scores;

    public ScoresPlot(double[][] scores) {
      this.scores = scores;
      setPreferredSize(new Dimension(400, 400));
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;

      // Plot the scores as points
      for (int i = 0; i < scores.length; i++) {
        int x = (int) scores[i][0]; // x-coordinate of the point
        int y = (int) scores[i][1]; // y-coordinate of the point
        g2d.setColor(Color.BLUE);
        g2d.fillOval(x - 3, y - 3, 6, 6); // Draw a small circle for each point
      }
    }
  }

}
