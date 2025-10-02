package no.imr.tools.jogl.volumetric;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.JoglDisposable;
import no.imr.tools.jogl.Shader;
import no.imr.tools.jogl.node.JoglDisplayNode;
import no.imr.tools.jogl.texture.Texture1DInt;
import no.imr.tools.jogl.texture.Texture3DFloat;
import no.imr.tools.logging.Log;
import no.imr.tools.math.linalg.LinalgUtils;
import no.imr.tools.math.linalg.Vec3;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public abstract class BaseVolumeDisplayer extends JoglDisplayNode {
   protected Texture3DFloat texture3D = new Texture3DFloat();
   protected Texture1DInt texture1D = new Texture1DInt();
   protected boolean disabled = false;
   protected boolean userInteractiveMode = false;
   protected final Shader shader = new Shader();
   protected final String vertexShaderSource;
   protected final String fragmentShaderSource;

   protected BaseVolumeDisplayer(String vertexShaderSource, String fragmentShaderSource) {
      this.vertexShaderSource = vertexShaderSource;
      this.fragmentShaderSource = fragmentShaderSource;
   }

   public void setTexture3D(Texture3DFloat texture3D) {
      this.texture3D = texture3D;
   }

   public Texture3DFloat getTexture3D() {
      return texture3D;
   }

   public void setColormapTexture(Texture1DInt texture1D) {
      this.texture1D = texture1D;
   }

   public Texture1DInt getColormapTexture() {
      return texture1D;
   }

   public static float[] computeCornerParams(Vec3[] coords, Vec3 pos, Vec3 norm) {
      return new float[]{
            LinalgUtils.distanceToPlane(coords[0], pos, norm),
            LinalgUtils.distanceToPlane(coords[1], pos, norm),
            LinalgUtils.distanceToPlane(coords[2], pos, norm),
            LinalgUtils.distanceToPlane(coords[3], pos, norm),
            LinalgUtils.distanceToPlane(coords[4], pos, norm),
            LinalgUtils.distanceToPlane(coords[5], pos, norm),
            LinalgUtils.distanceToPlane(coords[6], pos, norm),
            LinalgUtils.distanceToPlane(coords[7], pos, norm),
      };
   }

   protected abstract void doDisplay(GL2 gl);

   protected static void drawTexturedPolygons(List<MarchingCube.Polygon> polygons, GL2 gl, Vec3 offset) {
      for (MarchingCube.Polygon polygon : polygons) {
         gl.glBegin(GL_POLYGON);
         for (Vec3 vec3 : polygon.getPoints()) {
            Vec3 texCoord = vec3.plus(offset);
            gl.glTexCoord3f(texCoord.x(), texCoord.y(), texCoord.z());
            gl.glVertex3f(vec3.x(), vec3.y(), vec3.z());
         }
         gl.glEnd();
      }
   }

   protected static void drawOutlinePolygons(List<MarchingCube.Polygon> polygons, GL2 gl, Vec3 offset) {
      for (MarchingCube.Polygon polygon : polygons) {
         gl.glBegin(GL_LINES);
         for (Vec3 vec3 : polygon.getPoints()) {
            gl.glVertex3f(vec3.x(), vec3.y(), vec3.z());
         }
         gl.glEnd();
      }
   }

   public boolean isUserInteractiveMode() {
      return userInteractiveMode;
   }

   @Override
   public void init(GL2 gl, List<JoglDisposable> disposables) {
      String version = gl.glGetString(GL_VERSION);
      String[] split = version.split("\\.");
      if (Integer.parseInt(split[0]) < 2 && !disabled) {
         Log.global.warning("Volume visualization requires at least OpenGL 2.0. Your version is " + version);
         disabled = true;
         return;
      }
      shader.init(gl, disposables, vertexShaderSource, fragmentShaderSource);
      initAllTextures(gl, disposables);
   }

   protected abstract void initAllTextures(GL2 gl, List<JoglDisposable> disposables);
}
