package no.imr.lsss.server.pojo;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static com.jogamp.opengl.GL2.*;

public final class GraphicsInfo {
   public ApiGraphicsEnvironment localGraphicsEnvironment = new ApiGraphicsEnvironment(GraphicsEnvironment.getLocalGraphicsEnvironment());
   public ApiToolkit defaultToolkit = new ApiToolkit(Toolkit.getDefaultToolkit());
   public ApiOpenGL openGL = new ApiOpenGL();

   public GraphicsInfo() {
   }

   public static final class ApiGraphicsEnvironment {
      public ApiRectangle maximumWindowBounds;
      public ApiPoint centerPoint;
      public int defaultScreenDevice;
      public List<ApiGraphicsDevice> screenDevices;

      public ApiGraphicsEnvironment(GraphicsEnvironment graphicsEnvironment) {
         maximumWindowBounds = new ApiRectangle(graphicsEnvironment.getMaximumWindowBounds());
         centerPoint = new ApiPoint(graphicsEnvironment.getCenterPoint());
         defaultScreenDevice = List.of(graphicsEnvironment.getScreenDevices()).indexOf(graphicsEnvironment.getDefaultScreenDevice());
         screenDevices = Arrays.stream(graphicsEnvironment.getScreenDevices())
               .map(ApiGraphicsDevice::new)
               .toList();
      }
   }

   public static final class ApiGraphicsDevice {
      public String idString;
      public int availableAcceleratedMemory;
      public ApiGraphicsConfiguration defaultConfiguration;
      public DisplayMode displayMode;

      public ApiGraphicsDevice(GraphicsDevice graphicsDevice) {
         idString = graphicsDevice.getIDstring();
         availableAcceleratedMemory = graphicsDevice.getAvailableAcceleratedMemory();
         defaultConfiguration = new ApiGraphicsConfiguration(graphicsDevice.getDefaultConfiguration());
         displayMode = graphicsDevice.getDisplayMode();
      }
   }

   public static final class ApiGraphicsConfiguration {
      public ApiRectangle bounds;
      public ApiTransform defaultTransform;
      public ApiTransform normalizingTransform;

      public ApiGraphicsConfiguration(GraphicsConfiguration graphicsConfiguration) {
         bounds = new ApiRectangle(graphicsConfiguration.getBounds());
         defaultTransform = new ApiTransform(graphicsConfiguration.getDefaultTransform());
         normalizingTransform = new ApiTransform(graphicsConfiguration.getNormalizingTransform());
      }
   }

   public static final class ApiPoint {
      public int x;
      public int y;

      public ApiPoint(Point point) {
         x = point.x;
         y = point.y;
      }
   }

   public static final class ApiRectangle {
      public int x;
      public int y;
      public int width;
      public int height;

      public ApiRectangle(Rectangle rectangle) {
         x = rectangle.x;
         y = rectangle.y;
         width = rectangle.width;
         height = rectangle.height;
      }
   }

   public static final class ApiTransform {
      public double scaleX;
      public double scaleY;

      public ApiTransform(AffineTransform transform) {
         scaleX = transform.getScaleX();
         scaleY = transform.getScaleY();
      }
   }

   public static final class ApiToolkit {
      public Dimension bestCursorSize;
      public int maximumCursorColors;
      public int screenResolution;

      public ApiToolkit(Toolkit toolkit) {
         bestCursorSize = toolkit.getBestCursorSize(32, 32);
         maximumCursorColors = toolkit.getMaximumCursorColors();
         screenResolution = toolkit.getScreenResolution();
      }
   }

   public static final class ApiOpenGL {
      public @Nullable String vendor;
      public @Nullable String renderer;
      public @Nullable String version;
      public @Nullable String shadingLanguageVersion;
      public @Nullable List<String> extensions;
      public @Nullable String error;

      public ApiOpenGL() {
         try {
            GLProfile glProfile = GLProfile.get(GLProfile.GL2);
            GLCapabilities capabilities = new GLCapabilities(glProfile);
            GLAutoDrawable drawable = GLDrawableFactory.getFactory(glProfile).createOffscreenAutoDrawable(null, capabilities, null, 1, 1);
            drawable.addGLEventListener(new GLEventListener() {
               @Override
               public void init(GLAutoDrawable drawable) {
                  GL2 gl = (GL2) drawable.getGL();
                  vendor = gl.glGetString(GL_VENDOR);
                  renderer = gl.glGetString(GL_RENDERER);
                  version = gl.glGetString(GL_VERSION);
                  shadingLanguageVersion = gl.glGetString(GL_SHADING_LANGUAGE_VERSION);
                  int[] numExtensions = new int[1];
                  gl.glGetIntegerv(GL_NUM_EXTENSIONS, numExtensions, 0);
                  extensions = IntStream.range(0, numExtensions[0])
                        .mapToObj(i -> gl.glGetStringi(GL_EXTENSIONS, i))
                        .toList();
               }

               @Override
               public void dispose(GLAutoDrawable drawable) {
               }

               @Override
               public void display(GLAutoDrawable drawable) {
               }

               @Override
               public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
               }
            });
            drawable.display();
            drawable.destroy();
         } catch (Exception e) {
            error = e.toString();
         }
      }
   }
}
