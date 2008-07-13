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

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.project.impl.StorableScan;

import com.thoughtworks.xstream.XStream;

/**
 * Customized instance of XStream
 */
public class MZmineXStream extends XStream {

    private ReferenceConverter referenceConverter;

    /**
     * Create an XStream instance and set its parameters
     */
    public MZmineXStream() {

        // Register aliases
        alias("simpleScan", SimpleScan.class);
        alias("peakListRow", SimplePeakListRow.class);
        alias("storableScan", StorableScan.class);
        alias("dataPoint", SimpleDataPoint.class);
        alias("mzPeak", SimpleMzPeak.class);
        alias("rawDataFile", RawDataFileImpl.class);
        alias("isotopePattern", SimpleIsotopePattern.class);
        alias("chromatographicPeak", SimpleChromatographicPeak.class);

        // Set referencing mode to IDs, to avoid evaluating complicated XPath expressions
        setMode(XStream.ID_REFERENCES);

        omitField(DefaultListModel.class, "listenerList");
        
        // Create a referencing converter
        referenceConverter = new ReferenceConverter(getMapper(),
                getReflectionProvider());

        // Register our custom converters
        registerConverter(referenceConverter);
        registerConverter(new RangeConverter());
        registerConverter(new IntArrayConverter());
        registerConverter(new SimpleMzPeakConverter());
        registerConverter(new SimpleDataPointConverter());

    }

    /**
     * Returns the number of serialized Scan instances
     */
    public int getNumOfSerializedScans() {
        return referenceConverter.getNumOfSerializedScans();

    }

    /**
     * Returns the number of serialized PeakListRow instances
     */
    public int getNumOfSerializedRows() {
        return referenceConverter.getNumOfSerializedRows();
    }
    
    /**
     * Returns the number of serialized Scan instances
     */
    public int getNumOfDeserializedScans() {
        return referenceConverter.getNumOfDeserializedScans();

    }

    /**
     * Returns the number of serialized PeakListRow instances
     */
    public int getNumOfDeserializedRows() {
        return referenceConverter.getNumOfDeserializedRows();
    }

}
