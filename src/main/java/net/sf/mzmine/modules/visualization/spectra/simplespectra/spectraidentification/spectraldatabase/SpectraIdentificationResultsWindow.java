package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBEntry;
import net.sf.mzmine.modules.visualization.spectra.mirrorspectra.MirrorScanWindow;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;

public class SpectraIdentificationResultsWindow extends JFrame {
  /**
   * Window to show all spectral database matches from selected scan
   * 
   * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
   */

  private static final long serialVersionUID = 1L;
  private JPanel pnGrid;
  private Font titleFont = new Font("Dialog", Font.BOLD, 18);
  private Font regularFont = new Font("Dialog", Font.PLAIN, 16);

  public SpectraIdentificationResultsWindow(Scan scan, Map<Double, SpectralDBEntry> matches) {
    setBackground(Color.WHITE);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setMinimumSize(new Dimension(800, 600));
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());

    pnGrid = new JPanel();
    // any number of rows
    pnGrid.setLayout(new GridLayout(0, 1, 0, 25));
    pnGrid.setAutoscrolls(true);

    JScrollPane scrollPane = new JScrollPane(pnGrid);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    // add charts
    for (Map.Entry<Double, SpectralDBEntry> match : matches.entrySet()) {
      setScanAndShow(scan, match.getValue(), match.getKey());
    }

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
    validate();
    repaint();
    pack();
  }

  public boolean setScanAndShow(Scan scan, SpectralDBEntry ident, Double simScore) {
    // clear
    pnGrid.add(addSpectra(scan, ident, simScore));
    // show1
    pnGrid.revalidate();
    pnGrid.repaint();
    return true;
  }



  private JPanel addSpectra(Scan scan, SpectralDBEntry ident, Double simScore) {
    JPanel panel = new JPanel(new BorderLayout());

    JSplitPane spectrumPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    // set meta data from identity
    JPanel metaDataPane = new JPanel(new GridLayout(0, 1));
    // generate random color which is not too dark
    Random r1 = new Random();
    Color randomCol = Color.getHSBColor(r1.nextFloat(), // random hue, color
        1.0f, // full saturation, 1.0 for 'colorful' colors, 0.0 for grey
        0.6f // 1.0 for bright, 0.0 for black
    );
    // metaDataPane.add(colorBox);
    JTextArea titleArea = new JTextArea("Name: " + ident.getField(DBEntryField.NAME).toString());
    titleArea.setBackground(randomCol);
    titleArea.setFont(titleFont);
    titleArea.setForeground(Color.WHITE);

    metaDataPane.add(titleArea);
    metaDataPane.add(new JLabel("Entry ID: " + ident.getField(DBEntryField.ENTRY_ID).toString()));
    metaDataPane.add(new JLabel(
        "Cosine similarity: " + MZmineCore.getConfiguration().getRTFormat().format(simScore)));
    metaDataPane.add(new JLabel("Synonym: " + ident.getField(DBEntryField.SYNONYM)));
    metaDataPane.add(new JLabel("Comment: " + ident.getField(DBEntryField.COMMENT)));

    metaDataPane.add(new JLabel("Iontype: " + ident.getField(DBEntryField.IONTYPE)));

    metaDataPane.add(new JLabel("Retention time: " + ident.getField(DBEntryField.RT)));

    metaDataPane.add(new JLabel("MZ: " + ident.getField(DBEntryField.MZ)));

    metaDataPane.add(new JLabel("Charge: " + ident.getField(DBEntryField.CHARGE)));

    metaDataPane.add(new JLabel("Ion mode: " + ident.getField(DBEntryField.ION_MODE)));

    metaDataPane
        .add(new JLabel("Collision energy: " + ident.getField(DBEntryField.COLLISION_ENERGY)));

    metaDataPane.add(new JLabel("Formula: " + ident.getField(DBEntryField.FORMULA)));

    metaDataPane.add(new JLabel("Molar weight: " + ident.getField(DBEntryField.MOLWEIGHT)));


    // to do: more data
    // get spectra window
    MirrorScanWindow mirrorWindow = new MirrorScanWindow();
    mirrorWindow.setScans(scan, ident);

    // get spectra plot
    EChartPanel spectraPlots = mirrorWindow.getMirrorSpecrumPlot();

    // set up renderer
    PseudoSpectraRenderer renderer1 = new PseudoSpectraRenderer(Color.blue, false);
    PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(randomCol, false);

    spectraPlots.getChart().getLegend().setVisible(true);
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) spectraPlots.getChart().getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    XYPlot spectrumPlot = (XYPlot) domainPlot.getSubplots().get(0);
    spectrumPlot.setRenderer(renderer1);
    XYPlot databaseSpectrumPlot = (XYPlot) domainPlot.getSubplots().get(1);
    databaseSpectrumPlot.setRenderer(renderer2);
    spectraPlots.setPreferredSize(new Dimension(800, 400));
    spectrumPane.add(spectraPlots);
    spectrumPane.setResizeWeight(0.8);
    spectrumPane.setEnabled(true);
    spectrumPane.setDividerSize(5);
    spectrumPane.add(metaDataPane);
    panel.add(spectrumPane);
    panel.setBorder(BorderFactory.createLineBorder(Color.black));
    return panel;
  }

}
