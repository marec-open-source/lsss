package no.imr.tools.database;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public final class DatabaseClassChecker {
   private final Class<? extends BaseDatabaseObject> clazz;
   private @Nullable Method idSetter;
   private final List<Method> otherSetters = new ArrayList<>();

   private DatabaseClassChecker(Class<? extends BaseDatabaseObject> clazz) throws ReflectiveOperationException {
      this.clazz = clazz;

      try {
         findSetters();
         check();
      } catch (ReflectiveOperationException e) {
         Log.global.log(Level.WARNING, clazz.toString(), e);
         throw e;
      }
   }

   public static void checkAll(Collection<Class<? extends BaseDatabaseObject>> databaseClasses) throws ReflectiveOperationException {
      for (Class<? extends BaseDatabaseObject> databaseClass : databaseClasses) {
         new DatabaseClassChecker(databaseClass);
      }
   }

   private void check() throws ReflectiveOperationException {
      BaseDatabaseObject a = clazz.getDeclaredConstructor().newInstance();
      BaseDatabaseObject b = clazz.getDeclaredConstructor().newInstance();

      checkSamePrimaryKey("", a, b, true);
      checkEquals("", a, b, true);

      Set<Class<?>> pkClasses = Set.of(String.class);

      assertNotNull(idSetter);
      Class<?> pkClass = idSetter.getParameterTypes()[0];
      if (pkClass.isPrimitive() || pkClasses.contains(pkClass)) {
         invokeSetter(a, idSetter);
         checkSamePrimaryKey(idSetter.toString(), a, b, false);
         checkEquals(idSetter.toString(), a, b, false);
         invokeSetter(b, idSetter);
         checkSamePrimaryKey(idSetter.toString(), a, b, true);
         checkEquals(idSetter.toString(), a, b, true);
      } else {
         String expectedPKClassName = clazz.getName() + "PK";
         if (!expectedPKClassName.equals(pkClass.getName())) {
            Class<?> expectedPKClass = pkClass.getClassLoader().loadClass(expectedPKClassName);
            assertEquals(getFieldsAndMethods(expectedPKClass), getFieldsAndMethods(pkClass), expectedPKClassName);
         }

         Object aPK = pkClass.getDeclaredConstructor().newInstance();
         Object bPK = pkClass.getDeclaredConstructor().newInstance();
         idSetter.invoke(a, aPK);
         idSetter.invoke(b, bPK);
         checkEquals(idSetter.toString(), a, b, true);
         checkStringSetter(idSetter.toString(), a, idSetter);

         int pkSetterCount = 0;
         for (Method pkSetter : pkClass.getMethods()) {
            if (pkSetter.getName().startsWith("set")) {
               Class<?> argumentClass = pkSetter.getParameterTypes()[0];
               assertTrue(argumentClass.isPrimitive() || pkClasses.contains(argumentClass), pkSetter.toString());
               pkSetterCount++;
               invokeSetter(aPK, pkSetter);
               checkSamePrimaryKey(pkSetter.toString(), a, b, false);
               checkEquals(pkSetter.toString(), a, b, false);
               invokeSetter(bPK, pkSetter);
               checkSamePrimaryKey(pkSetter.toString(), a, b, true);
               checkEquals(pkSetter.toString(), a, b, true);
               checkStringSetter(pkSetter.toString(), aPK, pkSetter);
            }
         }
         assertEquals(getNonStaticDeclaredFields(pkClass).size(), pkSetterCount);
      }

      for (Method setter : otherSetters) {
         invokeSetter(a, setter);
         checkSamePrimaryKey(setter.toString(), a, b, true);
         checkEquals(setter.toString(), a, b, !isContentSetter(setter));
         invokeSetter(b, setter);
         checkSamePrimaryKey(setter.toString(), a, b, true);
         checkEquals(setter.toString(), a, b, true);
         checkStringSetter(setter.toString(), a, setter);
      }
   }

   private static Set<String> getFieldsAndMethods(Class<?> clazz) {
      Set<String> names = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
         names.add(method.getName());
      }
      for (Field field : getNonStaticDeclaredFields(clazz)) {
         names.add(field.getName());
      }
      return names;
   }

   private void findSetters() throws NoSuchMethodException {
      for (Method method : clazz.getDeclaredMethods()) {
         if (!method.getName().startsWith("get") || (method.getModifiers() & Modifier.PRIVATE) != 0) {
            // Ignore.
            continue;
         }
         boolean hasId = method.isAnnotationPresent(Id.class);
         boolean hasEmbeddedId = method.isAnnotationPresent(EmbeddedId.class);
         if (hasId || hasEmbeddedId) {
            if (hasId) {
               assertFalse(hasEmbeddedId, method::toString);
               assertFalse(BaseCompDatabaseObject.class.isAssignableFrom(clazz), method::toString);
               assertNotEquals("getCompId", method.getName(), method::toString);
            } else {
               assertTrue(BaseCompDatabaseObject.class.isAssignableFrom(clazz), method::toString);
               assertEquals("getCompId", method.getName(), method::toString);
               if ((method.getReturnType().getModifiers() & Modifier.ABSTRACT) != 0) {
                  // Ignore
                  continue;
               }
               assertTrue(method.getReturnType().isAnnotationPresent(Embeddable.class), method::toString);
            }
            assertNull(idSetter, method::toString);
            idSetter = clazz.getMethod("set" + method.getName().substring(3), method.getReturnType());
         } else {
            otherSetters.add(clazz.getMethod("set" + method.getName().substring(3), method.getReturnType()));
         }
      }

      assertNotNull(idSetter, clazz.toString());
      assertEquals(getNonStaticDeclaredFields(clazz).size(), otherSetters.size() + 1, clazz.toString());
   }

   private static List<Field> getNonStaticDeclaredFields(Class<?> clazz) {
      // This method necessary since jacoco adds static field.
      return Stream.of(clazz.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .toList();
   }

   private static void invokeSetter(Object object, Method setter) throws ReflectiveOperationException {
      Class<?> argumentClass = setter.getParameterTypes()[0];
      if (argumentClass.equals(short.class)) {
         setter.invoke(object, (short) 1);
      } else if (argumentClass.equals(int.class)) {
         setter.invoke(object, 1);
      } else if (argumentClass.equals(float.class)) {
         setter.invoke(object, 1f);
      } else if (argumentClass.equals(String.class)) {
         setter.invoke(object, "x");
      } else if (argumentClass.equals(Set.class)) {
         setter.invoke(object, new HashSet<>());
      } else {
         setter.invoke(object, argumentClass.getDeclaredConstructor().newInstance());
      }
   }

   private static boolean isContentSetter(Method setter) {
      Class<?> argumentClass = setter.getParameterTypes()[0];
      return argumentClass.equals(short.class)
            || argumentClass.equals(int.class)
            || argumentClass.equals(float.class)
            || argumentClass.equals(String.class);
   }

   private static void checkSamePrimaryKey(String message, BaseDatabaseObject a, BaseDatabaseObject b, boolean expectSame) {
      if (expectSame) {
         assertEquals(a.primaryKey(), b.primaryKey(), message);
         assertEquals(b.primaryKey(), a.primaryKey(), message);
      } else {
         assertNotEquals(a.primaryKey(), b.primaryKey(), message);
         assertNotEquals(b.primaryKey(), a.primaryKey(), message);
      }
   }

   private static void checkEquals(String message, Object a, Object b, boolean equals) {
      if (equals) {
         assertEquals(a, b, message);
         assertEquals(b, a, message);
         assertEquals(a.hashCode(), b.hashCode(), message);
      } else {
         assertNotEquals(a, b, message);
         assertNotEquals(b, a, message);
      }
   }

   private static void checkStringSetter(String message, Object object, Method setter) throws ReflectiveOperationException {
      if (setter.getParameterTypes()[0] != String.class) {
         return;
      }
      Method getter = object.getClass().getMethod("get" + setter.getName().substring(3));
      Object originalValue = getter.invoke(object);
      assertNotNull(originalValue, message);
      setter.invoke(object, (Object) null);
      assertEquals("", getter.invoke(object), message);
      setter.invoke(object, originalValue);
   }
}
