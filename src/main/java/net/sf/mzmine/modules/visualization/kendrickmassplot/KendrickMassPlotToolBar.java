package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * Kendrick mass plot toolbar class
 */
public class KendrickMassPlotToolBar extends JToolBar {

    private static final long serialVersionUID = 1L;
    static final Icon blockSizeIcon = new ImageIcon("icons/thicknessicon.png");
    static final Icon backColorIcon = new ImageIcon("icons/bgicon.png");
    static final Icon gridIcon = new ImageIcon("icons/tableselectionicon");

    public KendrickMassPlotToolBar(ActionListener masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        GUIUtils.addButton(this, null, blockSizeIcon, masterFrame,
                "TOGGLE_BLOCK_SIZE", "Toggle block size");

        addSeparator();

        GUIUtils.addButton(this, null, backColorIcon, masterFrame,
                "TOGGLE_BACK_COLOR", "Toggle background color white/black");

        addSeparator();

        GUIUtils.addButton(this, null, backColorIcon, masterFrame,
                "TOGGLE_GRID", "Toggle grid");

        addSeparator();

    }

}
