/**
 * 
 */
package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import sunutils.ExampleFileFilter;

/**
 * 
 */
public class FileOpenDialog extends JDialog implements ActionListener {

    private JFileChooser fileChooser;
    private JCheckBox preloadCheckBox;

    public FileOpenDialog() {

        super(MainWindow.getInstance(), "Please select data files to open",
                true);

        fileChooser = new JFileChooser();

        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addActionListener(this);

        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.setDescription("NetCDF files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("xml");
        filter.addExtension("mzxml");
        filter.setDescription("MZXML files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.addExtension("xml");
        filter.addExtension("mzxml");
        filter.setDescription("All raw data files");
        fileChooser.setFileFilter(filter);

        preloadCheckBox = new JCheckBox(
                "Preload all data into memory? (use with caution)");
        preloadCheckBox.setMargin(new Insets(10, 10, 10, 10));

        add(fileChooser, BorderLayout.CENTER);
        add(preloadCheckBox, BorderLayout.SOUTH);

        pack();

        setLocationRelativeTo(MainWindow.getInstance());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
        if (command.equals("ApproveSelection")) {

            File[] selectedFiles = fileChooser.getSelectedFiles();

            PreloadLevel preloadLevel = PreloadLevel.NO_PRELOAD;
            if (preloadCheckBox.isSelected())
                preloadLevel = PreloadLevel.PRELOAD_ALL_SCANS;
            IOController.getInstance().openFiles(selectedFiles, preloadLevel);

        }

        // discard this dialog
        dispose();

    }

}
