/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;

class Trace {

  private Long id = null;
  private String description = null;
  private String instrument = null;
  private String instrumentId = null;
  private Long type = null;
  private Long unit = null;
  private Double timeOffset = null;
  private Long color = null;

  public Trace() {
  }

  public boolean isValid() {
    // other types are not interesting
    if (id == null || description == null || instrument == null || type == null || unit == null) {
      return false;
    }

    if ((getChomatogramType() == ChromatogramType.UNKNOWN && !"Pump_pressure".equals(description))
        || getChomatogramType().equals(ChromatogramType.UNKNOWN)) {
      return false;
    }
    return true;
  }

  public ChromatogramType getChomatogramType() {
    if (type == null) {
      return ChromatogramType.UNKNOWN;
    }
    return switch (type.intValue()) {
      case 0 -> ChromatogramType.UNKNOWN; // None
      case 1 -> ChromatogramType.BPC; // any MS
      case 2 -> ChromatogramType.ABSORPTION; // DAD/PDA
      case 3 -> ChromatogramType.ABSORPTION; // UV trace
      case 4 -> ChromatogramType.PRESSURE; // pump pressure
      case 5 -> ChromatogramType.UNKNOWN; // solvent composition
      case 6 -> ChromatogramType.FLOW_RATE; // flow rate
      case 7 -> ChromatogramType.UNKNOWN; // temperature
      default -> ChromatogramType.UNKNOWN; // user defined
    };
  }

  public String getConvertedRangeUnit() {
    return BrukerUtils.unitToString(unit().intValue());
  }

  public void setUnit(Long unit) {
    this.unit = unit;
  }

  public String getConvertedRangeLabel() {
    return BrukerUtils.unitToLabel(unit.intValue());
  }

  public Long id() {
    return id;
  }

  public String description() {
    return description;
  }

  public String instrument() {
    return instrument;
  }

  public String instrumentId() {
    return instrumentId;
  }

  public Long type() {
    return type;
  }

  public Long unit() {
    return unit;
  }

  public Double timeOffset() {
    return timeOffset;
  }

  public Long color() {
    return color;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setInstrument(String instrument) {
    this.instrument = instrument;
  }

  public void setInstrumentId(String instrumentId) {
    this.instrumentId = instrumentId;
  }

  public void setType(Long type) {
    this.type = type;
  }

  public void setTimeOffset(Double timeOffset) {
    this.timeOffset = timeOffset;
  }

  public void setColor(Long color) {
    this.color = color;
  }
}
