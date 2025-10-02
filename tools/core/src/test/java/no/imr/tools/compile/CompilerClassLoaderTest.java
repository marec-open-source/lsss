package no.imr.tools.compile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CompilerClassLoaderTest {
   @Test
   void simple() throws CompileException {
      assertNotNull(CompilerClassLoader.instantiate(Object.class, "T", """
            public class T {
            }
            """));
   }

   @Test
   void unusedStaticImportStar() throws CompileException {
      assertNotNull(CompilerClassLoader.instantiate(Object.class, "T", """
            import static java.lang.Math.*;
            public class T {
            }
            """));
   }

   @Test
   void syntaxError() {
      CompileException e = assertThrows(CompileException.class, () -> {
         CompilerClassLoader.instantiate(Object.class, "T", """
               public clazz T {
               }
               """);
      });
      assertNull(e.getCause());
   }

   @Test
   void reflectionError() {
      CompileException e = assertThrows(CompileException.class, () -> {
         CompilerClassLoader.instantiate(Object.class, "T", """
               public class T {
                  private T() {
                  }
               }
               """);
      });
      assertInstanceOf(IllegalAccessException.class, e.getCause());
   }

   @Test
   void classCastError() {
      CompileException e = assertThrows(CompileException.class, () -> {
         CompilerClassLoader.instantiate(Runnable.class, "T", """
               public class T {
               }
               """);
      });
      assertInstanceOf(ClassCastException.class, e.getCause());
   }

   @Test
   void innerClass() throws CompileException {
      String source = """
            package no.cmr.test;
            public class OuterClass implements Runnable {
               private double x;
               private int n = 314;
               public void run() { x = 2 + 0.7; n = 1828; }
               public String toString() { return x + " ; " + new Inner().getN(); }
               private class Inner { int getN() { return n; } }
            }
            """;
      String className = "no.cmr.test.OuterClass";

      Runnable obj = CompilerClassLoader.instantiate(Runnable.class, className, source);
      assertEquals("0.0 ; 314", obj.toString());

      obj.run();
      assertEquals("2.7 ; 1828", obj.toString());
   }

   @Test
   void baseClass() throws CompileException {
      String source = """
            package no.imr.tools.compile;
            public class SubClass extends CompilerClassLoaderTest.BaseClass {
               public int getInt() {
                  return 2000 + n;
               }
            }
            """;
      String className = "no.imr.tools.compile.SubClass";

      BaseClass obj = CompilerClassLoader.instantiate(BaseClass.class, className, source);
      assertEquals(6, obj.n);
      assertEquals(2006, obj.getInt());
   }

   public abstract static class BaseClass {
      protected int n = 6;

      protected BaseClass() {
      }

      public abstract int getInt();
   }
}
