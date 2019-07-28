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
import net.sf.mzmine.util.components.MultipleSelectionComponent;

public class FeaturesSelectionDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private Logger LOG = Logger.getLogger(this.getClass().getName());
    public List<Feature> selectedFeatures;
    public MultipleSelectionComponent<Feature> peakListRowSelectionBox;
    public JComboBox<Object> rawDataFileSelectionBox;
    private final JButton okButton;
    private final JButton cancelButton;
    private boolean returnState = true;

    public FeaturesSelectionDialog() {
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        String[] allPeakListStrings = new String[allPeakLists.length];
        for (int i = 0; i < allPeakLists.length; i++) {
            allPeakListStrings[i] = allPeakLists[i].toString();
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel panel1 = new JPanel(new BorderLayout());
        JPanel panel2 = new JPanel(new BorderLayout());
        JPanel panel3 = new JPanel(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
        JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                allPeakListStrings);
        peakListsComboBox.setToolTipText("Peak Lists Selection Box");
        mainPanel.add(panel1, BorderLayout.NORTH);
        mainPanel.add(panel3, BorderLayout.SOUTH);
        mainPanel.add(panel2, BorderLayout.CENTER);
        JLabel label1 = new JLabel();
        label1.setText("Peak Lists");
        panel1.add(label1, BorderLayout.WEST);
        panel1.add(peakListsComboBox, BorderLayout.CENTER);
        JLabel label2 = new JLabel();
        label2.setText("Peak List Rows");
        JLabel label3 = new JLabel();
        label3.setText("Raw Data Files");
        final FeaturesSelectionDialog dialog = this;
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

                        panel2.add(label2, BorderLayout.WEST);
                        panel2.add(peakListRowSelectionBox,
                                BorderLayout.CENTER);
                        panel3.add(rawDataFileSelectionBox,
                                BorderLayout.CENTER);
                        panel3.add(label3, BorderLayout.WEST);

                        dialog.setSize(620, 350);
                        LOG.finest("PeakListRowComboBox is Added");
                    }
                    panel2.revalidate();
                }
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        this.add(buttonPane, BorderLayout.SOUTH);
        okButton = new JButton("OK");
        okButton.setEnabled(true);
        okButton.addActionListener(this);
        okButton.setFocusable(true);
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.addActionListener(this);
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
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
        if (src == okButton) {
            returnState = true;
            this.dispose();
        }
        if (src == cancelButton) {
            returnState = false;
            this.dispose();
        }

    }

}
