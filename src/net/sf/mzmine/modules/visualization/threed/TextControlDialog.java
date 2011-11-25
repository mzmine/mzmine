package net.sf.mzmine.modules.visualization.threed;

import visad.TextControl;
import visad.VisADException;
import visad.util.TextControlWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * A dialog that holds a VisAD TextControlWidget.
 *
 * @author $Author$
 * @version $Revision$
 */
public class TextControlDialog extends JDialog {

    private static final String TITLE = "Label Font";

    /**
     * Create the dialog.
     *
     * @param owner   parent window.
     * @param control the text control.
     * @throws VisADException  if there are VisAD problems.
     * @throws RemoteException if there are VisAD problems.
     */
    public TextControlDialog(final Window owner, final TextControl control) throws VisADException, RemoteException {

        super(owner, TITLE, ModalityType.APPLICATION_MODAL);

        // Layout dialog.
        final Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new TextControlWidget(control), Component.CENTER_ALIGNMENT);
        content.add(createDoneButton());
        pack();
    }

    /**
     * A button to close the dialog.
     *
     * @return the button.
     */
    private JButton createDoneButton() {

        final JButton button = new JButton("Done");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setVisible(false);
            }
        });
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(visible);
    }
}
