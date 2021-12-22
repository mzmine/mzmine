/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorElement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Multi stage fragmentation MSn
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MSnInfoImpl implements DDAMsMsInfo {

  public static final String XML_TYPE_NAME = "msn_multi_stage_info";
  private static final Logger logger = Logger.getLogger(MSnInfoImpl.class.getName());
  private final List<DDAMsMsInfo> precursors;

  public MSnInfoImpl(List<DDAMsMsInfo> precursors) {
    this.precursors = precursors;
  }

  public static DDAMsMsInfo fromMzML(List<MzMLPrecursorElement> precursorElements, int msLevel) {
    List<DDAMsMsInfo> precursors = new ArrayList<>();

    assert precursorElements.size() == msLevel : "MS level and precursor info does not match";

    int currentMsLevel = 2;
    for (var precursorElement : precursorElements) {
      DDAMsMsInfo info = DDAMsMsInfoImpl.fromMzML(precursorElement, currentMsLevel);
      precursors.add(info);
      currentMsLevel++;
    }
    return new MSnInfoImpl(precursors);
  }

  /**
   * List of precursors and sorted by MS level starting at MS2 -> MS3 ...
   *
   * @return list of MsMs info sorted by MS level
   */
  @NotNull
  public List<DDAMsMsInfo> getPrecursors() {
    return precursors;
  }

  @Override
  public @Nullable Float getActivationEnergy() {
    return getLastFragmentationStep().getActivationEnergy();
  }

  private DDAMsMsInfo getLastFragmentationStep() {
    return precursors.get(precursors.size() - 1);
  }

  @Override
  public double getIsolationMz() {
    return getLastFragmentationStep().getIsolationMz();
  }

  @Override
  public @Nullable Integer getPrecursorCharge() {
    return getLastFragmentationStep().getPrecursorCharge();
  }

  @Override
  public @Nullable Scan getParentScan() {
    return getLastFragmentationStep().getParentScan();
  }

  @Override
  public @Nullable Scan getMsMsScan() {
    return getLastFragmentationStep().getMsMsScan();
  }

  @Override
  public boolean setMsMsScan(Scan scan) {
    return getLastFragmentationStep().setMsMsScan(scan);
  }

  @Override
  public int getMsLevel() {
    return getLastFragmentationStep().getMsLevel();
  }

  @Override
  public ActivationMethod getActivationMethod() {
    return getLastFragmentationStep().getActivationMethod();
  }

  @Override
  public @Nullable Range<Double> getIsolationWindow() {
    return getLastFragmentationStep().getIsolationWindow();
  }

  @Override
  public void writeToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_TYPE_ATTRIBUTE, XML_TYPE_NAME);
    for (var p : precursors) {
      p.writeToXML(writer);
    }
    writer.writeEndElement();
  }

  @Override
  public MsMsInfo createCopy() {
    return new MSnInfoImpl(precursors);
  }

  public double getMS2PrecursorMz() {
    return precursors.get(0).getIsolationMz();
  }
}
