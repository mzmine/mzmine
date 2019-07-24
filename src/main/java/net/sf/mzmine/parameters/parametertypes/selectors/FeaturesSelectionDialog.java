package net.sf.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

public class FeaturesSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private Logger LOG = Logger.getLogger(this.getClass().getName());

    public FeaturesSelectionDialog() {
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        String[] allPeakListStrings = new String[allPeakLists.length];
        for (int i = 0; i < allPeakLists.length; i++) {
            allPeakListStrings[i] = allPeakLists[i].toString();
        }

        JPanel panel1 = new JPanel();
        this.add(panel1, BorderLayout.NORTH);
        JPanel panel2 = new JPanel();
        this.add(panel2, BorderLayout.SOUTH);
        JComboBox<Object> peakListsComboBox = new JComboBox<Object>(
                allPeakListStrings);
        panel1.add(peakListsComboBox);
        JComboBox<Object> peakListRowComboBox = new JComboBox<Object>();

        peakListsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOG.finest("Peak List Selected!");
                String str = (String) peakListsComboBox.getSelectedItem();
                for (int j = 0; j < allPeakLists.length; j++) {
                    if (str == allPeakLists[j].toString()) {
                        RawDataFile datafile = allPeakLists[j]
                                .getRawDataFile(0);
                        Feature[] features = allPeakLists[j].getPeaks(datafile);
                        String[] featuresStrings = new String[features.length];
                        for (int k = 0; k < features.length; k++) {
                            peakListRowComboBox.addItem(featuresStrings[k]);
                        }
                        panel2.add(peakListRowComboBox);
                        LOG.finest("PeakListRowComboBox is Added");
                    }
                }
            }
        });
        this.pack();
        Insets insets = this.getInsets();
        this.setSize(new Dimension(insets.left + insets.right + 600,
                insets.top + insets.bottom + 100));
        this.setLocationRelativeTo(null);
    }
}
