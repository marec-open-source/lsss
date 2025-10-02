package no.imr.tools.compile;

import java.util.Map;

final class InMemoryClassLoader extends ClassLoader {
   private final Map<String, byte[]> classes;

   InMemoryClassLoader(Map<String, byte[]> classes) {
      super(InMemoryClassLoader.class.getClassLoader());

      this.classes = classes;
   }

   Map<String, byte[]> getClasses() {
      return classes;
   }

   @Override
   protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] bytes = classes.get(name);
      if (bytes == null) {
         throw new ClassNotFoundException(name);
      }
      return defineClass(name, bytes, 0, bytes.length);
   }
}
