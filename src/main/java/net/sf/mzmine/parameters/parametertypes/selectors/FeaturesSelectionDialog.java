package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
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
    public List<Feature> selectedFeatures;
    public MultipleSelectionComponent<Feature> peakListRowSelectionBox;
    public JComboBox<Object> rawDataFileSelectionBox;
    private JPanel buttonPane;
    private JButton btnOk;
    private JButton btnCancel;
    private boolean returnState = true;
    private GridBagPanel mainPanel;

    public FeaturesSelectionDialog() {
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        String[] allPeakListStrings = new String[allPeakLists.length];
        for (int i = 0; i < allPeakLists.length; i++) {
            allPeakListStrings[i] = allPeakLists[i].toString();
        }

        mainPanel = new GridBagPanel();
        JPanel panel1 = new JPanel(new BorderLayout());
        JPanel panel2 = new JPanel(new BorderLayout());
        JPanel panel3 = new JPanel(new BorderLayout());
        this.add(mainPanel);
        JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                allPeakListStrings);
        peakListsComboBox.setToolTipText("Peak Lists Selection Box");
        JLabel peakListsLabel = new JLabel("Peak Lists");
        mainPanel.add(panel1, 0, 0);
        mainPanel.add(panel2, 0, 1);
        mainPanel.add(panel3, 0, 2);
        this.setSize(620, 350);
        panel1.add(peakListsLabel, BorderLayout.WEST);
        panel1.add(peakListsComboBox, BorderLayout.CENTER);
        JLabel peakListRowsLabel = new JLabel("Peak List Rows");
        JLabel rawDataFilesLabel = new JLabel("Raw data files");
        // RawDataFile datafile = allPeakLists[0].getRawDataFile(0);
        // Feature[] peakListRows = allPeakLists[0].getPeaks(datafile);
        // RawDataFile[] rawDataFiles = allPeakLists[0].getRawDataFiles();
        //
        // String[] featuresList = new String[peakListRows.length];
        //
        // for (int k = 0; k < peakListRows.length; k++) {
        // featuresList[k] = peakListRows[k].toString();
        // }
        // peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
        // featuresList);
        // peakListRowSelectionBox.setToolTipText("Features Selection Box");
        //
        // rawDataFileSelectionBox = new JComboBox<Object>(rawDataFiles);
        // panel2.add(peakListRowsLabel, BorderLayout.WEST);
        // panel2.add(peakListRowSelectionBox, BorderLayout.CENTER);
        // panel3.add(rawDataFileSelectionBox, BorderLayout.CENTER);
        // panel3.add(rawDataFilesLabel, BorderLayout.WEST);
        final FeaturesSelectionDialog dialog = this;
        // panel2.add(peakListRowsLabel, BorderLayout.WEST);
        // panel2.add(peakListRowSelectionBox, BorderLayout.CENTER);
        // panel3.add(rawDataFileSelectionBox, BorderLayout.CENTER);
        // panel3.add(rawDataFilesLabel, BorderLayout.WEST);

        peakListsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LOG.finest("Peak List Selected!");
                String str = (String) peakListsComboBox.getSelectedItem();
                panel2.removeAll();
                panel3.removeAll();
                for (int j = 0; j < allPeakLists.length; j++) {
                    if (str == allPeakLists[j].toString()) {
                        RawDataFile datafile = allPeakLists[j]
                                .getRawDataFile(0);
                        Feature[] peakListRows = allPeakLists[j]
                                .getPeaks(datafile);
                        RawDataFile[] rawDataFiles = allPeakLists[j]
                                .getRawDataFiles();

                        String[] featuresList = new String[peakListRows.length];

                        for (int k = 0; k < peakListRows.length; k++) {
                            featuresList[k] = peakListRows[k].toString();
                        }
                        peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                                featuresList);
                        peakListRowSelectionBox
                                .setToolTipText("Features Selection Box");

                        rawDataFileSelectionBox = new JComboBox<Object>(
                                rawDataFiles);

                        panel2.add(peakListRowsLabel, BorderLayout.WEST);
                        panel2.add(peakListRowSelectionBox,
                                BorderLayout.CENTER);
                        panel3.add(rawDataFileSelectionBox,
                                BorderLayout.CENTER);
                        panel3.add(rawDataFilesLabel, BorderLayout.WEST);
                        final int index = j;
                        rawDataFileSelectionBox
                                .addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        panel2.removeAll();
                                        RawDataFile dataFile = (RawDataFile) rawDataFileSelectionBox
                                                .getSelectedItem();
                                        Feature[] peakListRows = allPeakLists[index]
                                                .getPeaks(dataFile);
                                        for (int k = 0; k < peakListRows.length; k++) {
                                            featuresList[k] = peakListRows[k]
                                                    .toString();
                                        }
                                        peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                                                featuresList);
                                        peakListRowSelectionBox.setToolTipText(
                                                "Features Selection Box");
                                        panel2.add(peakListRowsLabel,
                                                BorderLayout.WEST);
                                        panel2.add(peakListRowSelectionBox,
                                                BorderLayout.CENTER);

                                        panel2.revalidate();
                                    }
                                });
                        dialog.setSize(620, 350);
                        LOG.finest("PeakListRowComboBox is Added");
                    }
                    panel2.revalidate();
                    panel3.revalidate();
                }
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        this.add(buttonPane, BorderLayout.SOUTH);
        btnOk = GUIUtils.addButton(buttonPane, "OK", null, this);

        btnCancel = GUIUtils.addButton(buttonPane, "Cancel", null, this);
        this.pack();
        Insets insets = this.getInsets();
        this.setSize(new Dimension(insets.left + insets.right + 600,
                insets.top + insets.bottom + 100));
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

    }

}
