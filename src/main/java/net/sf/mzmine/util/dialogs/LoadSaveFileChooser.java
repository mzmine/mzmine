package net.sf.mzmine.util.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * A JFileChooser with convenience functions for loading and saving files.
 *
 * @author $Author$
 * @version $Revision$
 */
public class LoadSaveFileChooser extends JFileChooser {

    /**
     * Prompt the user for the file to load.
     *
     * @param parent parent window for dialogs.
     * @return the selected file (or null if no choice is made).
     */
    public File getLoadFile(final Component parent) {

        // Clear default selection.
        setSelectedFile(new File(""));

        // Select a file.
        File file = null;
        if (showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {

            // Get the selected file.
            file = getSelectedFile();
        }

        return file;
    }

    /**
     * Prompt the user for the file to save to.  If the selected file exists then the user is prompted to confirm
     * overwriting it.  If the filename extension is missing it is added.
     *
     * @param parent    parent window for dialogs.
     * @param extension extension to append.
     * @return the selected file (or null if no choice is made).
     */
    public File getSaveFile(final Component parent, final String extension) {

        // Clear default selection.
        setSelectedFile(new File(""));

        // Select a file.
        File file = null;
        boolean done = false;
        while (!done) {
            done = true;
            if (showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {

                // Get the selected file.
                file = getSelectedFile();
                if (!file.exists()) {

                    file = applyFileNameExtension(file, extension);
                }

                // Does the file exist?
                if (file.exists() &&
                    JOptionPane.showConfirmDialog(parent,
                                                  "Do you want to overwrite the file?",
                                                  "File Exists",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {

                    // Ask again.
                    done = false;
                }
            }
        }

        return file;
    }

    /**
     * Returns the file with the correct extension.
     *
     * @param file      the file.
     * @param extension filename extension (without the '.').
     * @return if the file is not null and doesn't end with the correct extension then a new file with the extension
     *         appended to the name is returned, otherwise the file is returned unchanged.
     */
    private static File applyFileNameExtension(final File file, final String extension) {

        final String name = file.getName();
        final String dotExt = '.' + extension;
        return name.toLowerCase().endsWith(dotExt.toLowerCase()) ? file :
               new File(file.getParent(), name + (name.endsWith(".") ? extension : dotExt));
    }
}
