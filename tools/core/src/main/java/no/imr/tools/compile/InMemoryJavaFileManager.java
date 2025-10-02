package no.imr.tools.compile;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File manager directing output to {@link InMemoryClassFileObject}s.
 */
final class InMemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
   private final Map<String, InMemoryClassFileObject> classFileObjects = new HashMap<>();

   InMemoryJavaFileManager(JavaFileManager fileManager) {
      super(fileManager);
   }

   InMemoryClassLoader createClassLoader() {
      Map<String, byte[]> classes = classFileObjects.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                  Map.Entry::getKey,
                  entry -> entry.getValue().getBytes()
            ));
      return new InMemoryClassLoader(classes);
   }

   @Override
   public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
      InMemoryClassFileObject classFileObject = new InMemoryClassFileObject(classNameToFile(className, ".class"));
      classFileObjects.put(className, classFileObject);
      return classFileObject;
   }

   static URI classNameToFile(String className, String suffix) {
      String simpleName = className.substring(className.lastIndexOf('.') + 1);
      return URI.create(simpleName + suffix);
   }
}
