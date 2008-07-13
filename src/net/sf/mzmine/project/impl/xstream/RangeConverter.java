/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl.xstream;

import net.sf.mzmine.util.Range;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Simple converter for Range class
 */
public class RangeConverter implements Converter {

    public boolean canConvert(Class type) {
        return type.equals(Range.class);
    }

    public void marshal(Object original, final HierarchicalStreamWriter writer,
            final MarshallingContext context) {

        Range rng = (Range) original;

        String value = Float.toString(rng.getMin()) + ":"
                + Float.toString(rng.getMax());

        writer.setValue(value);

    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {

        String value[] = reader.getValue().split(":");

        Range rng = new Range(Float.valueOf(value[0]), Float.valueOf(value[1]));

        return rng;

    }

}
