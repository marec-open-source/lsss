package no.imr.tools.jogl.volumetric;

import com.jogamp.opengl.GL2;
import no.imr.tools.math.linalg.Vec3;

import static com.jogamp.opengl.GL2.*;

public final class DVRDisplayer extends VolumeDisplayer {
   public DVRDisplayer(String vertexShaderSource, String fragmentShaderSource) {
      super(vertexShaderSource, fragmentShaderSource);
   }

   @Override
   protected void doDisplay(GL2 gl) {
      Vec3 center = new Vec3(0.0f, 0.0f, 0.0f);
      Vec3 camPos = new Vec3(0, 0, 1);
      camPos = eyeToModel(camPos).minus(eyeToModel(center));

      gl.glEnable(GL_BLEND);
      gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

      //let the box dimension be large enough to allow all ping directions
      float v = 1.0f;
      Vec3[] coord = {
            new Vec3(-v, -v, -v), new Vec3(v, -v, -v), new Vec3(v, -v, v), new Vec3(-v, -v, v),
            new Vec3(-v, v, -v), new Vec3(v, v, -v), new Vec3(v, v, v), new Vec3(-v, v, v),
      };

      Vec3 norm = camPos.unit();

      float[] param = computeCornerParams(coord, center, norm);

      bindTextures(gl);
      gl.glColor4f(1.0f, 1.0f, 1.0f, 0.02f);

      float delta = isUserInteractiveMode() ? 0.01f : 0.001f;
      for (float value = -1f; value < 1f; value += delta) {
         MarchingCube mc = new MarchingCube(value, coord, param);

         drawTexturedPolygons(mc.getPolys(), gl);
      }
      releaseTextures(gl);
      gl.glDisable(GL_BLEND);
   }
}
