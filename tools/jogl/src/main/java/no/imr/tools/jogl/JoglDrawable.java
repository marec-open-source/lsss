package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;

@FunctionalInterface
public interface JoglDrawable {
   void draw(GL2 gl);
}
