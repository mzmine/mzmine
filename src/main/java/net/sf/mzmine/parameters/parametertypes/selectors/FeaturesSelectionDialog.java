package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.JToolBar;

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
        this.add(mainPanel, BorderLayout.CENTER);
        JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                allPeakListStrings);
        peakListsComboBox.setToolTipText("Peak Lists Selection Box");
        mainPanel.add(panel1, BorderLayout.NORTH);
        mainPanel.add(panel2, BorderLayout.SOUTH);
        JLabel label1 = new JLabel();
        label1.setText("Peak Lists");
        panel1.add(label1, BorderLayout.NORTH);
        panel1.add(peakListsComboBox, BorderLayout.CENTER);

        JLabel label2 = new JLabel();
        label2.setText("Features");
        final FeaturesSelectionDialog dialog = this;
        peakListsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LOG.finest("Peak List Selected!");
                String str = (String) peakListsComboBox.getSelectedItem();
                panel2.removeAll();
                for (int j = 0; j < allPeakLists.length; j++) {
                    if (str == allPeakLists[j].toString()) {
                        RawDataFile datafile = allPeakLists[j]
                                .getRawDataFile(0);
                        Feature[] features = allPeakLists[j].getPeaks(datafile);
                        String[] featuresList = new String[features.length];
                        for (int k = 0; k < features.length; k++) {
                            featuresList[k] = features[k].toString();
                        }
                        peakListRowSelectionBox = new MultipleSelectionComponent<Feature>(
                                featuresList);
                        peakListRowSelectionBox
                                .setToolTipText("Features Selection Box");
                        panel2.add(label2, BorderLayout.NORTH);
                        panel2.add(peakListRowSelectionBox, BorderLayout.SOUTH);
                        dialog.setSize(620, 320);
                        LOG.finest("PeakListRowComboBox is Added");
                    }
                    panel2.revalidate();
                }
            }
        });
        JToolBar buttonBar = new JToolBar();
        buttonBar.setLayout(new GridLayout());
        this.add(buttonBar, BorderLayout.SOUTH);
        okButton = new JButton("OK");
        okButton.setEnabled(true);
        okButton.addActionListener(this);
        okButton.setFocusable(true);
        // okButton.setRolloverEnabled(true);
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.addActionListener(this);
        buttonBar.add(okButton);
        buttonBar.add(cancelButton);
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
