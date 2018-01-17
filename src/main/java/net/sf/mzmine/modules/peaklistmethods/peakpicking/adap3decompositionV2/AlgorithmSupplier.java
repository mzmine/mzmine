/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public abstract class AlgorithmSupplier implements ActionListener {

    protected ParameterSet parameters = null;

    public abstract String getName();

    public abstract Parameter<?>[] getParameters();

    public abstract JPanel getPanel();

    public void updateData(@Nonnull DataProvider dataProvider) {
        this.parameters = dataProvider.getParameterSet();
    }
}
