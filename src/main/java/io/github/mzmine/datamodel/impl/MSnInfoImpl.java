/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Multi stage fragmentation MSn. The precursors that lead to this scan
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MSnInfoImpl implements DDAMsMsInfo {

  public static final String XML_TYPE_NAME = "msn_multi_stage_info";
  private static final Logger logger = Logger.getLogger(MSnInfoImpl.class.getName());
  /**
   * All the precursor stages that lead to this scan. An MS4 scan will have these isolation steps:
   * MS2 --> MS3 --> MS4 in this order.
   */
  private final List<DDAMsMsInfo> precursors;

  public MSnInfoImpl(List<DDAMsMsInfo> precursors) {
    this.precursors = precursors;
  }

  public static DDAMsMsInfo fromMzML(List<MzMLPrecursorElement> precursorElements, int msLevel) {
    assert precursorElements.size() == msLevel - 1 : "MS level and precursor info does not match";

    List<DDAMsMsInfo> precursors = new ArrayList<>();

    // we sort the precursor elements by the MS level defined as user parameter by msconvert
    // if not specified we use the scan reference - earlier scan should also be lower in level
    // if not specified we use the precursor mz
    Collections.sort(precursorElements);

    int currentMsLevel = 2;
    for (var precursorElement : precursorElements) {
      DDAMsMsInfo info = DDAMsMsInfoImpl.fromMzML(precursorElement, currentMsLevel);
      precursors.add(info);
      currentMsLevel++;
    }
    return new MSnInfoImpl(precursors);
  }

  /**
   * @param reader A reader at an {@link DDAMsMsInfoImpl} element.
   * @return A loaded {@link DDAMsMsInfoImpl}.
   */
  public static MSnInfoImpl loadFromXML(XMLStreamReader reader, RawDataFile file,
      List<RawDataFile> allProjectFiles) {
    List<DDAMsMsInfo> precursors = new ArrayList<>(4);
    int childrenOpen = 0;
    try {
      while (reader.hasNext()) {
        int next = reader.next();
        if (next == XMLEvent.END_ELEMENT && reader.getLocalName().equals(MsMsInfo.XML_ELEMENT)) {
          if (childrenOpen > 0) {
            childrenOpen--;
          } else {
            break;
          }
        }
        if (next != XMLEvent.START_ELEMENT) {
          continue;
        }

        final MsMsInfo loaded = MsMsInfo.loadFromXML(reader, file, allProjectFiles);
        if (loaded instanceof DDAMsMsInfo child) {
          childrenOpen++;
          precursors.add(child);
        } else {
          throw new IllegalStateException(
              "MSn info was not loaded correctly. Child was " + (loaded == null ? null
                  : loaded.getClass()));
        }
      }
    } catch (XMLStreamException ex) {
      logger.log(Level.WARNING, "Errow while loading MSn info. " + ex.getMessage(), ex);
      return null;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MSnInfoImpl mSnInfo = (MSnInfoImpl) o;
    return precursors.equals(mSnInfo.precursors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(precursors);
  }
}
