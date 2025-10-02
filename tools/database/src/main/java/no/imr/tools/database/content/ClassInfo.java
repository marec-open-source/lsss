package no.imr.tools.database.content;

import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class ClassInfo {
   final Class<? extends BaseDatabaseObject> databaseClass;
   private final Map<String, Optional<SetterInfo>> setterInfos;
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
            SetterInfo compIdSetter = DatabaseXmlContent.get(compIdInfo.compIdSetterInfos, name);
            if (compIdSetter != null) {
               compIdSetter.callSet(compId, value);
               continue;
            }
         }

         SetterInfo setter = DatabaseXmlContent.get(setterInfos, name);
         if (setter == null) {
            throw new ReflectiveOperationException("No setter " + name + " in " + databaseClass.getName());
         }
         setter.callSet(databaseObject, value);
      }

      return databaseObject;
   }

   private static Map<String, Optional<SetterInfo>> findSetters(Class<?> clazz) {
      Map<String, Optional<SetterInfo>> setters = new HashMap<>();
      for (Method method : clazz.getDeclaredMethods()) {
         if (!method.getName().startsWith("set")) {
            continue;
         }
         setters.put(method.getName().substring(3).toLowerCase(Locale.ENGLISH), Optional.of(new SetterInfo(method)));
      }
      return setters;
   }

   private static final class CompIdInfo {
      private final Class<?> compIdClass;
      private final Method setCompId;
      private final Map<String, Optional<SetterInfo>> compIdSetterInfos;

      private CompIdInfo(Class<? extends BaseDatabaseObject> databaseClass) throws NoSuchMethodException {
         compIdClass = databaseClass.getMethod("getCompId").getReturnType();
         compIdSetterInfos = findSetters(compIdClass);
         setCompId = databaseClass.getMethod("setCompId", compIdClass);
      }
   }
}
