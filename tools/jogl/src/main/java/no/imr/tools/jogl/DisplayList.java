package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.logging.Log;

import java.util.List;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL2.*;

public final class DisplayList implements JoglDisposable {
   private int displayList;
   private final Consumer<GL2> drawFunction;
   private boolean dirty;

   public DisplayList(Consumer<GL2> drawFunction) {
      this.drawFunction = drawFunction;
   }

   public void init(GL2 gl, List<JoglDisposable> disposeTasks) {
      displayList = gl.glGenLists(1);
      if (displayList == 0) {
         Log.global.warning("Error making display list");
      }
      dirty = true;
      disposeTasks.add(this);
   }

   @Override
   public void dispose(GL2 gl) {
      if (displayList != 0) {
         gl.glDeleteLists(displayList, 1);
         displayList = 0;
      }
   }

   public void triggerUpdate() {
      dirty = true;
   }

   public void draw(GL2 gl) {
      if (dirty) {
         dirty = false;
         compile(gl);
      }
      gl.glCallList(displayList);
   }

   private void compile(GL2 gl) {
      gl.glNewList(displayList, GL_COMPILE);
      drawFunction.accept(gl);
      gl.glEndList();
   }
}
