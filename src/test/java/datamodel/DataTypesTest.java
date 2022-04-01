package datamodel;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataTypesTest {

  private static final Logger logger = Logger.getLogger(DataTypesTest.class.getName());

  @Test
  public void testUniqueID() {

    // map unique ID to instance
    final HashMap<String, DataType<?>> map = new HashMap<>();

    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              final Class<?> clazz = classInfo.load();
              clazz.asSubclass(DataType.class);
              Object o = clazz.getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getUniqueID(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with unique ID " + dt.getUniqueID() + "\n"
                          + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
              if (!Modifier.isAbstract(classInfo.load().getModifiers()) && !classInfo.load()
                  .isInterface()) {
                Assertions.fail("Cannot instantiate DataType class " + classInfo.load().getName()
                    + ". Is the constructor not public?");
              }
            } catch (ClassCastException e) {
              // can go silent, not a DataType
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
              final Class<?> clazz = classInfo.load();
              clazz.asSubclass(DataType.class);
              Object o = clazz.getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getClass().getSimpleName(), dt);
                if (value != null) {
                  Assertions.fail(
                      "FATAL: Multiple data types with class name " + dt.getClass().getSimpleName()
                          + "\n" + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
              if (!Modifier.isAbstract(classInfo.load().getModifiers()) && !classInfo.load()
                  .isInterface()) {
                Assertions.fail("Cannot instantiate DataType class " + classInfo.load().getName()
                    + ". Is the constructor not public?");
              }
            } catch (ClassCastException e) {
              // can go silent, not a DataType
            }
          });
    } catch (IOException e) {
      Assertions.fail("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

}
