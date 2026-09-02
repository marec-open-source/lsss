package no.imr.tools.compile;

import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Loads classes from sources compiled at runtime.
 */
public final class CompilerClassLoader {
   private static final Path CACHE_DIR = Utils.getTmpDir().resolve("cache").resolve("compile");
   private static final byte[] CACHE_SALT = (Runtime.version().toString()
         + (Utils.IS_BUILT_VERSION ? Utils.BUILD_TIME : Utils.BUILD_TIME.truncatedTo(ChronoUnit.DAYS))).getBytes(Utils.UTF_8);


   static {
      Exec.schedule(CompilerClassLoader::cleanUpCache, 15, TimeUnit.SECONDS);
   }

   private CompilerClassLoader() {
   }

   private static ClassLoader create(String className, String source) throws CompileException {
      Path cacheFile = getCacheFile(className, source);
      try {
         return readCache(cacheFile);
      } catch (Exception e) {
         if (!FileUtils.notExists(e, cacheFile)) {
            Log.global.log(Level.WARNING, "Error reading cache file " + cacheFile, e);
         }
      }

      InMemoryClassLoader classLoader = compile(className, source);

      try {
         writeCache(classLoader, cacheFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing cache file " + cacheFile, e);
      }

      return classLoader;
   }

   private static InMemoryClassLoader compile(String className, String source) throws CompileException {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if (compiler == null) {
         throw new UnsupportedOperationException("No compiler");
      }

      DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
      StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Utils.UTF_8);
      try (InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager)) {
         List<String> options = List.of(
               "-classpath", System.getProperty("java.class.path"),
               "-nowarn"
         );
         URI sourceFile = InMemoryJavaFileManager.classNameToFile(className, ".java");
         JavaFileObject sourceFileObject = SimpleJavaFileObject.forSource(sourceFile, source);
         StringWriter out = new StringWriter();
         JavaCompiler.CompilationTask compilationTask = compiler.getTask(out, fileManager, diagnosticCollector, options, null, List.of(sourceFileObject));
         boolean success = compilationTask.call();
         if (!success) {
            throw new CompileException(diagnosticCollector, out.toString());
         }

         return fileManager.createClassLoader();
      } catch (IOException e) {
         throw new CompileException("Error closing compiler file manager", e);
      }
   }

   public static <T> T instantiate(Class<T> superClass, String className, String source) throws CompileException {
      ClassLoader classLoader = create(className, source);
      try {
         Class<?> subClass = classLoader.loadClass(className);
         Object instance = subClass.getDeclaredConstructor().newInstance();
         return superClass.cast(instance);
      } catch (ReflectiveOperationException | ClassCastException e) {
         throw new CompileException("Error instantiating compiled class", e);
      }
   }

   private static void writeCache(InMemoryClassLoader classLoader, Path file) throws IOException {
      Map<String, byte[]> classes = classLoader.getClasses();
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytesOut)) {
         out.writeInt(classes.size());
         for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
         }
      }
      FileUtils.replaceFileSafely(file, bytesOut.toByteArray());
   }

   private static InMemoryClassLoader readCache(Path file) throws IOException {
      // Read the whole file first so in.available() gives the exact remaining byte count.
      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(file)))) {
         int count = in.readInt();
         if (count < 0 || count > in.available()) {
            throw new IOException("Corrupt cache file " + file);
         }
         Map<String, byte[]> classMap = HashMap.newHashMap(count);
         for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            int length = in.readInt();
            if (length < 0 || length > in.available()) {
               throw new IOException("Corrupt cache file " + file);
            }
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            classMap.put(name, bytes);
         }
         return new InMemoryClassLoader(classMap);
      }
   }

   private static Path getCacheFile(String className, String source) {
      MessageDigest digest = Utils.getSha256();
      digest.update(className.getBytes(Utils.UTF_8));
      digest.update(source.getBytes(Utils.UTF_8));
      digest.update(CACHE_SALT);
      byte[] bytes = digest.digest();
      String fileName = HexFormat.of().formatHex(bytes) + ".dat";
      return CACHE_DIR.resolve(fileName);
   }

   private static void cleanUpCache() {
      if (Utils.isTestRun()) {
         return;
      }
      Instant keepTime = Instant.now().minus(10, ChronoUnit.DAYS);
      try {
         for (FileInfo fileInfo : FileUtils.listFilesWithAttributes(CACHE_DIR)) {
            if (fileInfo.attributes().lastAccessTime().toInstant().isBefore(keepTime)) {
               Files.deleteIfExists(fileInfo.file());
            }
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error cleaning cache dir " + CACHE_DIR, e);
      }
   }
}
