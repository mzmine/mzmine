/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * 
 */
class TICToolBar extends JToolBar {

    private JButton zoomOutButton, showSpectraButton;

    TICToolBar(TICVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomOutButton = new JButton(new ImageIcon("zoomouticon.png"));
        zoomOutButton.setEnabled(false);
        zoomOutButton.setActionCommand("ZOOM_OUT");
        zoomOutButton.addActionListener(masterFrame);

        showSpectraButton = new JButton(new ImageIcon("spectrumicon.png"));
        showSpectraButton.setEnabled(false);
        showSpectraButton.setActionCommand("SHOW_SPECTRUM");
        showSpectraButton.addActionListener(masterFrame);

        add(zoomOutButton);
        addSeparator();
        add(showSpectraButton);

    }

    void setZoomOutButton(boolean enabled) {
        zoomOutButton.setEnabled(enabled);
    }

    void setSpectraButton(boolean enabled) {
        showSpectraButton.setEnabled(enabled);
    }

}
