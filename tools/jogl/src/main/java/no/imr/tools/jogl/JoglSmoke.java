package no.imr.tools.jogl;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestExecutor;
import no.imr.tools.smoke.SwingSmokeTestRunnable;

final class JoglSmoke extends SwingSmokeTestRunnable {
   private JoglSmoke() {
   }

   @Override
   public void swingRun() {
      new GLU();
      new GLJPanel();
      new GLCanvas();
      assert !ColormapDataDisplayer.getFragmentSource().isEmpty();
      Log.global.info(OK + "JOGL");
   }

   public static void main(String[] args) {
      SmokeTestExecutor.execute(null, new JoglSmoke());
   }
}
