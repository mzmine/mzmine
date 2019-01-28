/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */package net.sf.mzmine.modules.visualization.multimsms.pseudospectra;

import java.util.HashMap;
import java.util.Map;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class PseudoSpectrumDataSet extends XYSeriesCollection {
  private static final long serialVersionUID = 1L;

  private Map<XYDataItem, String> annotation;

  public PseudoSpectrumDataSet(Comparable key, boolean autoSort) {
    super(new XYSeries(key, autoSort));
  }

  public void addDP(double x, double y, String ann) {
    XYDataItem dp = new XYDataItem(x, y);
    getSeries(0).add(dp);
    if (ann != null) {
      addAnnotation(dp, ann);
    }
  }

  /**
   * Add annotation
   * 
   * @param dp
   * @param ann
   */
  public void addAnnotation(XYDataItem dp, String ann) {
    if (annotation == null)
      this.annotation = new HashMap<>();
    annotation.put(dp, ann);
  }

  public String getAnnotation(int item) {
    if (annotation == null)
      return null;
    XYDataItem itemDataPoint = getSeries(0).getDataItem(item);
    for (XYDataItem key : annotation.keySet()) {
      if (Math.abs(key.getXValue() - itemDataPoint.getXValue()) < 0.0001)
        return annotation.get(key);
    }
    return null;
  }

  public void addIdentity(MZTolerance mzTolerance, AbstractMSMSIdentity ann) {
    if (ann instanceof AbstractMSMSDataPointIdentity)
      addDPIdentity(mzTolerance, (AbstractMSMSDataPointIdentity) ann);
    // TODO add diff identity
  }

  private void addDPIdentity(MZTolerance mzTolerance, AbstractMSMSDataPointIdentity ann) {
    XYSeries series = getSeries(0);
    for (int i = 0; i < series.getItemCount(); i++) {
      XYDataItem dp = series.getDataItem(i);
      if (mzTolerance.checkWithinTolerance(dp.getXValue(), ann.getMZ())) {
        addAnnotation(dp, ann.getName());
      }
    }
  }
}
