package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import no.imr.tools.range.FloatRange;

import static com.jogamp.opengl.GL2.*;

public final class Jogl2dModule extends JoglOrthographicModule {
   public Jogl2dModule() {
      super(null, FloatRange.of(-1, 1));
   }

   @Override
   public void init(GL2 gl) {
      super.init(gl);

      gl.glShadeModel(GL_FLAT);
      gl.glDisable(GL_DEPTH_TEST);
   }
}
