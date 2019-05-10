package net.sf.mzmine.modules.visualization.spectra.mirrorspectra;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class MirrorScanWindow extends JFrame {

  private JPanel contentPane;
  private EChartPanel mirrorSpecrumPlot;

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
    mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartPanel(labelA, precursorMZA, rtA, dpsA,
        labelB, precursorMZB, rtB, dpsB, false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
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
    contentPane.removeAll();
    mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartPanel(scan, mirror,
        scan.getScanDefinition(), mirror.getScanDefinition(), false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();

  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    contentPane.removeAll();
    mirrorSpecrumPlot =
        SpectrumChartFactory.createMirrorChartPanel(scan, mirror, labelA, labelB, false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
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


  public void setScans(Scan scan, SpectralDBEntry ident) {
    if (scan == null)
      return;
    // scan a
    double rtA = scan.getRetentionTime();
    DataPoint[] dpsA = scan.getDataPoints();

    //
    Double rtB = (Double) ident.getField(DBEntryField.RT).orElse(0d);
    DataPoint[] dpsB = ident.getDataPoints();
    this.setScans(scan.getScanDefinition(), 0.0, rtA, dpsA, "database", 0.0, rtB, dpsB);
  }

  public EChartPanel getMirrorSpecrumPlot() {
    return mirrorSpecrumPlot;
  }

  public void setMirrorSpecrumPlot(EChartPanel mirrorSpecrumPlot) {
    this.mirrorSpecrumPlot = mirrorSpecrumPlot;
  }

}
