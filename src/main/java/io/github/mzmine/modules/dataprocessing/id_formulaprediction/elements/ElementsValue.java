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
public final class ElementsValue {

    IIsotope chosenIsotope;
    String max;
    String min;

    ElementsValue(IIsotope iisotope, String max, String min)
    {

        this.chosenIsotope = iisotope;
        this.max = max;
        this.min = min;
    }

    String getMax(){return max;}
    String getMin(){return min;}
    IIsotope getIsotope(){return chosenIsotope;}
     void  setMax(String max){this.max = max;}
     void setMin(String min){ this.min = min;}
    void setIsotope(IIsotope iisotope){ this.chosenIsotope = iisotope;}


}
