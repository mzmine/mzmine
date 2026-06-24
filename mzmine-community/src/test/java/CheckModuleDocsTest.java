import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

public class CheckModuleDocsTest {

  private static final Logger logger = Logger.getLogger(CheckModuleDocsTest.class.getName());

  @Test
  @Disabled
  void testDocumentationLinks() {
//    MZmineTestUtil.startMzmineCore();

    try {
      ClassPath classPath = ClassPath.from(MZmineModule.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.modules").forEach(classInfo -> {
        try {
          Object o = classInfo.load().getDeclaredConstructor().newInstance();
          if (o instanceof MZmineModule mod) {
            final ParameterSet parameterSet = mod.getParameterSetClass().getDeclaredConstructor()
                .newInstance();
            final String helpUrl = parameterSet.getOnlineHelpUrl();
            if (helpUrl == null) {
              logger.info("Module\t" + mod.getName() + "\t" + mod.getClass().getName()
                  + "\thas no documentation link");
            }
          }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 ExceptionInInitializerError | NoSuchMethodException | NullPointerException |
                 NoClassDefFoundError | IllegalStateException e) {
          //               can go silent
          //              logger.log(Level.INFO, e.getMessage(), e);
        }
      });
    } catch (IOException e) {
      logger.severe("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }
}
