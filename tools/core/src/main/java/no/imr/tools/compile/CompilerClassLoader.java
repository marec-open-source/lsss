package no.imr.tools.compile;

import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
         if (Files.exists(cacheFile)) {
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
      InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager);

      List<String> options = List.of(
            "-classpath", System.getProperty("java.class.path"),
            "-nowarn");
      URI sourceFile = InMemoryJavaFileManager.classNameToFile(className, ".java");
      InMemorySourceFileObject inMemorySourceFileObject = new InMemorySourceFileObject(sourceFile, source);
      StringWriter out = new StringWriter();
      JavaCompiler.CompilationTask compilationTask = compiler.getTask(out, fileManager, diagnosticCollector, options, null, List.of(inMemorySourceFileObject));
      compilationTask.call();
      if (!diagnosticCollector.getDiagnostics().isEmpty()) {
         throw new CompileException(diagnosticCollector, out.toString());
      }

      return fileManager.createClassLoader();
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
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bytesOut)) {
         out.writeObject(classLoader.getClasses());
      }
      FileUtils.replaceFileSafely(file, bytesOut.toByteArray());
   }

   private static InMemoryClassLoader readCache(Path file) throws IOException, ClassNotFoundException {
      InMemoryClassLoader inMemoryClassLoader;
      try (ObjectInputStream in = new ObjectInputStream(FileUtils.newBufferedInputStream(file))) {
         @SuppressWarnings("unchecked")
         Map<String, byte[]> classMap = (Map<String, byte[]>) in.readObject();
         inMemoryClassLoader = new InMemoryClassLoader(classMap);
      }
      Exec.CACHED_THREAD_POOL.execute(() -> {
         try {
            FileUtils.setLastAccessed(file, System.currentTimeMillis());
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error setting last accessed time on " + file, e);
         }
      });
      return inMemoryClassLoader;
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
