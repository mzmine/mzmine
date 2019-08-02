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
    private List<Feature> selectedFeatures;
    private MultipleSelectionComponent<Feature> peakListRowSelectionBox;
    private JComboBox<Object> rawDataFileComboBox;
    private JComboBox<Object> peakListsComboBox;
    private JPanel buttonPane;
    private JButton btnOk;
    private JButton btnCancel;
    private boolean returnState = true;
    private PeakList[] allPeakLists;
    private String[] allPeakListStrings;
    private GridBagPanel mainPanel;
    private int selectedIndex = 0;
    private JPanel panel0;
    private JPanel panel1;
    private JPanel panel2;

    public FeaturesSelectionDialog() {
        allPeakLists = MZmineCore.getProjectManager().getCurrentProject()
                .getPeakLists();
        allPeakListStrings = new String[allPeakLists.length];
        for (int i = 0; i < allPeakLists.length; i++) {
            allPeakListStrings[i] = allPeakLists[i].toString();
        }

        mainPanel = new GridBagPanel();
        panel0 = new JPanel(new BorderLayout());
        panel1 = new JPanel(new BorderLayout());
        panel2 = new JPanel(new BorderLayout());
        this.add(mainPanel);
        peakListsComboBox = new JComboBox<Object>(allPeakListStrings);
        peakListsComboBox.setToolTipText("Peak Lists Selection Box");
        peakListsComboBox.addActionListener(this);
        JLabel peakListsLabel = new JLabel("Peak Lists");

        mainPanel.add(panel0, 0, 0);
        mainPanel.add(panel1, 0, 1);
        mainPanel.add(panel2, 0, 2);

        panel0.add(peakListsLabel, BorderLayout.WEST);
        panel0.add(peakListsComboBox, BorderLayout.CENTER);

        RawDataFile datafile = allPeakLists[0].getRawDataFile(0);
        Feature[] peakListRows = allPeakLists[0].getPeaks(datafile);
        RawDataFile[] rawDataFiles = allPeakLists[0].getRawDataFiles();

        String[] featuresList = new String[peakListRows.length];

        for (int k = 0; k < peakListRows.length; k++) {
            featuresList[k] = peakListRows[k].toString();
        }
        peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                featuresList);
        peakListRowSelectionBox.setToolTipText("Features Selection Box");

        rawDataFileComboBox = new JComboBox<Object>(rawDataFiles);
        rawDataFileComboBox.setToolTipText("Raw data files Selection Box");
        rawDataFileComboBox.addActionListener(this);
        JLabel peakListRowsLabel = new JLabel("Peak List Rows");
        JLabel rawDataFilesLabel = new JLabel("Raw data files");
        panel1.add(peakListRowsLabel, BorderLayout.WEST);
        panel1.add(peakListRowSelectionBox, BorderLayout.CENTER);
        panel2.add(rawDataFileComboBox, BorderLayout.CENTER);
        panel2.add(rawDataFilesLabel, BorderLayout.WEST);

        buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        this.add(buttonPane, BorderLayout.SOUTH);
        btnOk = GUIUtils.addButton(buttonPane, "OK", null, this);

        btnCancel = GUIUtils.addButton(buttonPane, "Cancel", null, this);
        this.pack();
        this.setSize(620, 350);
        this.setLocationRelativeTo(null);
    }

    public MultipleSelectionComponent<Feature> getMultipleSelectionComponent() {
        return peakListRowSelectionBox;
    }

    public List<Feature> getSelectedFeatures() {
        return selectedFeatures;
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
            Feature[] peakListRows = allPeakLists[selectedIndex]
                    .getPeaks(dataFile);
            String[] featuresList = new String[peakListRows.length];
            for (int k = 0; k < peakListRows.length; k++) {
                featuresList[k] = peakListRows[k].toString();
            }
            JLabel peakListRowsLabel = new JLabel("Peak List Rows");
            peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                    featuresList);
            peakListRowSelectionBox.setToolTipText("Features Selection Box");
            panel1.add(peakListRowsLabel, BorderLayout.WEST);
            panel1.add(peakListRowSelectionBox, BorderLayout.CENTER);

            panel1.revalidate();
        }
        if (src == peakListsComboBox) {
            LOG.finest("Peak List Selected!");
            String str = (String) peakListsComboBox.getSelectedItem();
            panel1.removeAll();
            panel2.removeAll();
            for (int j = 0; j < allPeakLists.length; j++) {
                if (str == allPeakLists[j].toString()) {
                    selectedIndex = j;
                    RawDataFile datafile = allPeakLists[j].getRawDataFile(0);
                    Feature[] peakListRows = allPeakLists[j].getPeaks(datafile);
                    RawDataFile[] rawDataFiles = allPeakLists[j]
                            .getRawDataFiles();

                    String[] featuresList = new String[peakListRows.length];

                    for (int k = 0; k < peakListRows.length; k++) {
                        featuresList[k] = peakListRows[k].toString();
                    }
                    peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                            featuresList);
                    peakListRowSelectionBox.setSize(600, 200);
                    peakListRowSelectionBox
                            .setToolTipText("Features Selection Box");

                    rawDataFileComboBox = new JComboBox<Object>(rawDataFiles);
                    rawDataFileComboBox.addActionListener(this);
                    JLabel peakListRowsLabel = new JLabel("Peak List Rows");
                    JLabel rawDataFilesLabel = new JLabel("Raw data files");
                    panel1.add(peakListRowsLabel, BorderLayout.WEST);
                    panel1.add(peakListRowSelectionBox, BorderLayout.CENTER);
                    panel2.add(rawDataFileComboBox, BorderLayout.CENTER);
                    panel2.add(rawDataFilesLabel, BorderLayout.WEST);
                    this.setSize(620, 350);
                    LOG.finest("PeakListRowComboBox is Added");
                }
                panel1.revalidate();
                panel2.revalidate();
            }

        }
    }
}
