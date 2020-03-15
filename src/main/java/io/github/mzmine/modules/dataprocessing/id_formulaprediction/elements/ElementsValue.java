/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.elements;
import org.openscience.cdk.interfaces.IIsotope;
public class ElementsValue {

    IIsotope chosenIsotope;
    int Max = 100;
    int Min = 0;

    ElementsValue(IIsotope iisotope, int max, int min)
    {

        this.chosenIsotope = iisotope;
        this.Max = max;
        this.Min = min;
    }

    int getMax(){return Max;}
    int getMin(){return Min;}
    IIsotope getIsotope(){return chosenIsotope;}

}
