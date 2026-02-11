package no.imr.tools.database.content;

import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class ClassInfo {
   private final Class<? extends BaseDatabaseObject> databaseClass;
   private final Map<String, SetterInfo> setterInfos;
   private final @Nullable CompIdInfo compIdInfo;

   ClassInfo(Class<? extends BaseDatabaseObject> databaseClass) throws ReflectiveOperationException {
      this.databaseClass = databaseClass;

      setterInfos = findSetters(databaseClass);

      if (BaseCompDatabaseObject.class.isAssignableFrom(databaseClass)) {
         compIdInfo = new CompIdInfo(databaseClass);
      } else {
         compIdInfo = null;
      }
   }

   BaseDatabaseObject newInstance(Element element) throws ReflectiveOperationException {
      BaseDatabaseObject databaseObject = databaseClass.getDeclaredConstructor().newInstance();
      Object compId;
      if (compIdInfo != null) {
         compId = compIdInfo.compIdClass.getDeclaredConstructor().newInstance();
         compIdInfo.setCompId.invoke(databaseObject, compId);
      } else {
         compId = null;
      }

      for (Attribute attribute : element.attributes()) {
         String name = attribute.getName();
         String value = attribute.getValue();

         if (compIdInfo != null) {
            SetterInfo compIdSetter = compIdInfo.compIdSetterInfos.get(name);
            if (compIdSetter != null) {
               compIdSetter.callSet(compId, value);
               continue;
            }
         }

         SetterInfo setter = setterInfos.get(name);
         if (setter == null) {
            throw new ReflectiveOperationException("No setter for " + name + " in " + databaseClass.getName());
         }
         setter.callSet(databaseObject, value);
      }

      return databaseObject;
   }

   private static Map<String, SetterInfo> findSetters(Class<?> clazz) {
      Map<String, SetterInfo> setters = new HashMap<>();
      for (Method method : clazz.getDeclaredMethods()) {
         String name = method.getName();
         if (!name.startsWith("set")) {
            continue;
         }
         String property = Character.toLowerCase(name.charAt(3)) + name.substring(4);
         setters.put(property, new SetterInfo(method));
      }
      return setters;
   }

   private static final class CompIdInfo {
      private final Class<?> compIdClass;
      private final Method setCompId;
      private final Map<String, SetterInfo> compIdSetterInfos;

      private CompIdInfo(Class<? extends BaseDatabaseObject> databaseClass) throws NoSuchMethodException {
         compIdClass = databaseClass.getMethod("getCompId").getReturnType();
         compIdSetterInfos = findSetters(compIdClass);
         setCompId = databaseClass.getMethod("setCompId", compIdClass);
      }
   }
}
