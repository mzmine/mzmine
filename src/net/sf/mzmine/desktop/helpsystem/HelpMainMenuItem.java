/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.helpsystem;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.help.HelpBroker;

import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.impl.MainMenu;
import net.sf.mzmine.main.MZmineCore;

public class HelpMainMenuItem {

    public void addMenuItem(MainMenu menu) {

        try {

            MZmineHelpSet hs = MZmineCore.getHelpImp().getHelpSet();

            if (hs == null)
                return;

            HelpBroker hb = hs.createHelpBroker();
            ActionListener helpListener = new HelpListener(hb);

            hs.setHomeID("net/sf/mzmine/desktop/helpsystem/AboutText.html");

            menu.addMenuItem(MZmineMenu.HELPSYSTEM, "About MZmine 2 ...",
                    "About MZmine 2...", KeyEvent.VK_A, false, helpListener,
                    null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
