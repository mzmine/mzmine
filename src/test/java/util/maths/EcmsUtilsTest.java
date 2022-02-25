package util.maths;

import io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential.EcmsUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class EcmsUtilsTest {

  @Test
  public void testtubingVolume() {
    Assertions.assertEquals(9.500765233078083, EcmsUtils.getTubingVolume(750d, 0.127d));
  }

  @Test
  public void testDelayTime() {
    Assertions.assertEquals(57.0045913984685d, EcmsUtils.getDelayTime(10d / 60, 9.500765233078083));
  }

  @Test
  public void testPotential() {
    Assertions.assertEquals(1889.977043d, EcmsUtils.getPotentialAtRt(7.25f, 57.0045914d, 5));
  }
}
