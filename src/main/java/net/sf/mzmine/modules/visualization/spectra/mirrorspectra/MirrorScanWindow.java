package net.sf.mzmine.modules.visualization.spectra.mirrorspectra;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBPeakIdentity;
import net.sf.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;

public class MirrorScanWindow extends JFrame {

  private JPanel contentPane;

  /**
   * Create the frame.
   */
  public MirrorScanWindow() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 800, 800);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);
  }

  public void setScans(String labelA, double precursorMZA, double rtA, DataPoint[] dpsA,
      String labelB, double precursorMZB, double rtB, DataPoint[] dpsB) {
    contentPane.removeAll();
    EChartPanel pn = SpectrumChartFactory.createMirrorChartPanel(labelA, precursorMZA, rtA, dpsA,
        labelB, precursorMZB, rtB, dpsB, false, true);
    contentPane.add(pn, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  /**
   * Set scan and mirror scan and create chart
   * 
   * @param scan
   * @param mirror
   */
  public void setScans(Scan scan, Scan mirror) {

  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    contentPane.removeAll();
    EChartPanel pn =
        SpectrumChartFactory.createMirrorChartPanel(scan, mirror, labelA, labelB, false, true);
    contentPane.add(pn, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  /**
   * Based on a data base match to a spectral library
   * 
   * @param row
   * @param db
   */
  public void setScans(PeakListRow row, SpectralDBPeakIdentity db) {
    Scan scan = row.getBestFragmentation();
    if (scan == null)
      return;
    // scan a
    double precursorMZA = scan.getPrecursorMZ();
    double rtA = scan.getRetentionTime();
    DataPoint[] dpsA = scan.getDataPoints();

    //
    double precursorMZB = db.getEntry().getPrecursorMZ();
    Double rtB = (Double) db.getEntry().getField(DBEntryField.RT).orElse(0d);
    DataPoint[] dpsB = db.getEntry().getDataPoints();
    this.setScans("", precursorMZA, rtA, dpsA, "database", precursorMZB, rtB, dpsB);
  }
}
