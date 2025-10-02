package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;

@FunctionalInterface
public interface JoglDisposable {
   void dispose(GL2 gl);
}
