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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class IntArrayConverter implements Converter {

    public boolean canConvert(Class type) {
        return (type.isArray() && type.getComponentType().equals(Integer.TYPE));
    }

    public void marshal(Object original, final HierarchicalStreamWriter writer,
            final MarshallingContext context) {

        int originalArray[] = (int[]) original;

        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < originalArray.length; i++) {
            if (i > 0) str.append(":");
            str.append(String.valueOf(originalArray[i]));
        }

        writer.setValue(str.toString());

    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {

        String value[] = reader.getValue().split(":");

        int intArray[] = new int[value.length];
        for (int i = 0; i < value.length; i++) {
            intArray[i] = Integer.parseInt(value[i]);
        }

        return intArray;

    }

}
