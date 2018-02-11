package net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.datamodel.PeakListRow;

public class NameItemLabelGenerator implements XYItemLabelGenerator {

  private PeakListRow rows[];

  public NameItemLabelGenerator(PeakListRow rows[]) {
    this.rows = rows;
  }

  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    return rows[item].getPreferredPeakIdentity().getName();
  }

}
