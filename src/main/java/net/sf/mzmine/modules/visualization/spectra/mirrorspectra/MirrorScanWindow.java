package net.sf.mzmine.modules.visualization.spectra.mirrorspectra;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;

public class MirrorScanWindow extends JFrame {

  private JPanel contentPane;

  /**
   * Create the frame.
   */
  public MirrorScanWindow() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 800, 800);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);
  }

  /**
   * Set scan and mirror scan and create chart
   * 
   * @param scan
   * @param mirror
   */
  public void setScans(Scan scan, Scan mirror) {
    contentPane.removeAll();
    EChartPanel pn = SpectrumChartFactory.createMirrorChartPanel(scan, mirror, false, true);
    contentPane.add(pn, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }
}
