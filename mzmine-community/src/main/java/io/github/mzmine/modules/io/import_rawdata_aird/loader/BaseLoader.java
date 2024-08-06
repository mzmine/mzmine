package io.github.mzmine.modules.io.import_rawdata_aird.loader;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.enums.MsLevel;

public class BaseLoader {

  private static final Logger logger = Logger.getLogger(BaseLoader.class.getName());

  public static DDAMsMsInfoImpl buildMsMsInfo(String activator, Float energy, WindowRange range,
      Scan parentScan) {
    Double precursorMz = range.getMz();
    Integer charge = range.getCharge() == 0 ? null : range.getCharge();
    ActivationMethod method = ActivationMethod.UNKNOWN;
    try {
      method = ActivationMethod.valueOf(activator);
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    if (precursorMz == null) {
      return null;
    }

    DDAMsMsInfoImpl msMsInfo = new DDAMsMsInfoImpl(precursorMz, charge, energy, null, parentScan,
        MsLevel.MS2.getCode(), method, Range.closed(range.getStart(), range.getEnd()));
    return msMsInfo;
  }
}
