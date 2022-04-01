package datamodel;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataTypesTest {

  @Test
  public void testUniqueID() {

    // map unique ID to instance
    final HashMap<String, DataType<?>> map = new HashMap<>();

    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              Object o = classInfo.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getUniqueID(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with unique ID " + dt.getUniqueID() + "\n"
                          + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
              //               can go silent
              //              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
    } catch (IOException e) {
      Assertions.fail("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

  @Test
  public void testSimpleClassName() {
    // map unique ID to instance
    final HashMap<String, DataType<?>> map = new HashMap<>();

    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              Object o = classInfo.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getClass().getSimpleName(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with class name " + dt.getClass().getSimpleName()
                          + "\n" + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            }
          });
    } catch (IOException e) {
      Assertions.fail("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

}
