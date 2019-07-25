package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.components.MultipleSelectionComponent;

public class FeaturesSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private Logger LOG = Logger.getLogger(this.getClass().getName());
    public List<Feature> selectedFeatures;
    public MultipleSelectionComponent<Feature> peakListRowSelectionBox;

    public FeaturesSelectionDialog() {
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        String[] allPeakListStrings = new String[allPeakLists.length];
        for (int i = 0; i < allPeakLists.length; i++) {
            allPeakListStrings[i] = allPeakLists[i].toString();
        }

        JPanel panel = new JPanel(new BorderLayout());
        this.add(panel, BorderLayout.NORTH);
        JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                allPeakListStrings);
        panel.add(peakListsComboBox, BorderLayout.NORTH);
        final FeaturesSelectionDialog dialog = this;
        peakListsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LOG.finest("Peak List Selected!");
                String str = (String) peakListsComboBox.getSelectedItem();
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

                        panel.add(peakListRowSelectionBox, BorderLayout.SOUTH);
                        dialog.setSize(620, 270);

                        LOG.finest("List of selected features is:"
                                + selectedFeatures);
                        LOG.finest("PeakListRowComboBox is Added");
                    }
                }
            }
        });
        // selectedFeatures = peakListRowSelectionBox.getSelectedValues();
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

}
