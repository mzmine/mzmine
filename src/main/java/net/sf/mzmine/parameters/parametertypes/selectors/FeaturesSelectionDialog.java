package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.GridBagPanel;
import net.sf.mzmine.util.components.MultipleSelectionComponent;

public class FeaturesSelectionDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private Logger LOG = Logger.getLogger(this.getClass().getName());
    private MultipleSelectionComponent<Feature> featuresSelectionBox;
    private JComboBox<RawDataFile> rawDataFileComboBox;
    private JComboBox<PeakList> peakListComboBox;
    private JPanel buttonPane;
    private JButton btnOk;
    private JButton btnCancel;
    private boolean returnState = true;
    private PeakList[] allPeakLists;
    private GridBagPanel mainPanel;
    private int selectedIndex = 0;
    private JPanel panel0;
    private JPanel panel1;
    private JPanel panel2;

    public FeaturesSelectionDialog() {

        mainPanel = new GridBagPanel();
        panel0 = new JPanel(new BorderLayout());
        panel1 = new JPanel(new BorderLayout());
        panel2 = new JPanel(new BorderLayout());
        mainPanel.add(panel0, 0, 0);
        mainPanel.add(panel1, 0, 1);
        mainPanel.add(panel2, 0, 2);
        this.add(mainPanel);

        allPeakLists = MZmineCore.getProjectManager().getCurrentProject()
                .getPeakLists();
        peakListComboBox = new JComboBox<PeakList>(allPeakLists);
        peakListComboBox.setToolTipText("Peak Lists Selection Box");
        peakListComboBox.addActionListener(this);
        JLabel peakListsLabel = new JLabel("Peak Lists");

        panel0.add(peakListsLabel, BorderLayout.WEST);
        panel0.add(peakListComboBox, BorderLayout.CENTER);

        RawDataFile datafile = allPeakLists[0].getRawDataFile(0);
        Feature[] features = allPeakLists[0].getPeaks(datafile);
        featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                features);
        featuresSelectionBox.setToolTipText("Features Selection Box");
        JLabel featuresLabel = new JLabel("Features");
        panel1.add(featuresSelectionBox, BorderLayout.CENTER);
        panel1.add(featuresLabel, BorderLayout.WEST);

        RawDataFile[] rawDataFiles = allPeakLists[0].getRawDataFiles();
        rawDataFileComboBox = new JComboBox<RawDataFile>(rawDataFiles);
        rawDataFileComboBox.setToolTipText("Raw data files Selection Box");
        rawDataFileComboBox.addActionListener(this);
        JLabel rawDataFilesLabel = new JLabel("Raw data files");
        panel2.add(rawDataFileComboBox, BorderLayout.CENTER);
        panel2.add(rawDataFilesLabel, BorderLayout.WEST);

        buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        btnOk = GUIUtils.addButton(buttonPane, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonPane, "Cancel", null, this);
        this.add(buttonPane, BorderLayout.SOUTH);
        this.pack();
        this.setSize(620, 400);
        this.setLocationRelativeTo(null);
    }

    public List<Feature> getSelectedFeatures() {
        return featuresSelectionBox.getSelectedValues();
    }

    public PeakList getSelectedPeakList() {
        return (PeakList) peakListComboBox.getSelectedItem();
    }

    public RawDataFile getSelectedRawDataFile() {
        return (RawDataFile) rawDataFileComboBox.getSelectedItem();
    }

    public boolean getReturnState() {
        return returnState;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == btnOk) {
            returnState = true;
            this.dispose();
        }
        if (src == btnCancel) {
            returnState = false;
            this.dispose();
        }
        if (src == rawDataFileComboBox) {
            panel1.removeAll();
            RawDataFile dataFile = (RawDataFile) rawDataFileComboBox
                    .getSelectedItem();
            Feature[] features = allPeakLists[selectedIndex].getPeaks(dataFile);
            JLabel featuresLabel = new JLabel("Features");
            featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                    features);
            featuresSelectionBox.setToolTipText("Features Selection Box");
            panel1.add(featuresLabel, BorderLayout.WEST);
            panel1.add(featuresSelectionBox, BorderLayout.CENTER);
            panel1.revalidate();
        }
        if (src == peakListComboBox) {
            LOG.finest("Peak List Selected!");
            PeakList peakList = (PeakList) peakListComboBox.getSelectedItem();
            panel1.removeAll();
            panel2.removeAll();

            for (int j = 0; j < allPeakLists.length; j++) {
                if (peakList.equals(allPeakLists[j])) {
                    selectedIndex = j;
                    RawDataFile datafile = allPeakLists[j].getRawDataFile(0);
                    Feature[] features = allPeakLists[j].getPeaks(datafile);
                    featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                            features);
                    featuresSelectionBox
                            .setToolTipText("Features Selection Box");
                    JLabel featuresLabel = new JLabel("Features");
                    panel1.add(featuresLabel, BorderLayout.WEST);
                    panel1.add(featuresSelectionBox, BorderLayout.CENTER);

                    RawDataFile[] rawDataFiles = allPeakLists[j]
                            .getRawDataFiles();
                    rawDataFileComboBox = new JComboBox<RawDataFile>(
                            rawDataFiles);
                    rawDataFileComboBox
                            .setToolTipText("Raw data files Selection Box");
                    rawDataFileComboBox.addActionListener(this);
                    JLabel rawDataFilesLabel = new JLabel("Raw data files");
                    panel2.add(rawDataFileComboBox, BorderLayout.CENTER);
                    panel2.add(rawDataFilesLabel, BorderLayout.WEST);
                    this.setSize(620, 400);
                    LOG.finest("PeakListRowComboBox is Added");
                }
                panel1.revalidate();
                panel2.revalidate();
            }
        }
    }
}
