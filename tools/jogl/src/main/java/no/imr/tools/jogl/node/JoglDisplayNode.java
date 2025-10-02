package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;

public abstract non-sealed class JoglDisplayNode extends JoglNode {
   private boolean transparent;

   protected JoglDisplayNode() {
   }

   public void setTransparent() {
      transparent = true;
   }

   protected abstract void draw(GL2 gl);

   @Override
   protected boolean drawOpaque(GL2 gl) {
      if (transparent) {
         return false;
      }
      draw(gl);
      return true;
   }

   @Override
   protected void drawTransparent(GL2 gl) {
      if (transparent) {
         draw(gl);
      }
   }
}
