/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters;

import java.awt.Window;
import java.util.Collection;

import net.sf.mzmine.util.ExitCode;

import org.w3c.dom.Element;

/**
 * This class represents a general parameter set of a module. Typical module
 * will use a SimpleParameterSet instance.
 * 
 * @param <T>
 */
public interface ParameterSet {

    public Parameter<?>[] getParameters();

    public <T extends Parameter<?>> T getParameter(T parameter);

    public void loadValuesFromXML(Element element);

    public void saveValuesToXML(Element element);

    public boolean checkParameterValues(Collection<String> errorMessages);

    public ParameterSet cloneParameterSet();

    /**
     * Represent method's parameters and their values in human-readable format
     */
    public String toString();

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired);

}
