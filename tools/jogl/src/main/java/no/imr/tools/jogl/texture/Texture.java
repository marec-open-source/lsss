package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.JoglDisposable;

import java.nio.IntBuffer;
import java.util.List;

public abstract class Texture implements JoglDisposable {
   private int textureID;
   private boolean dirty;

   Texture() {
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
      IntBuffer buffer = IntBuffer.allocate(1);
      gl.glGenTextures(1, buffer);
      textureID = buffer.get(0);
      dirty = true;
      disposables.add(this);
   }

   @Override
   public void dispose(GL2 gl) {
      gl.glDeleteTextures(1, new int[]{textureID}, 0);
      textureID = 0;
   }

   int getTextureID() {
      return textureID;
   }

   void markDirty() {
      dirty = true;
   }

   public void bindTexture(GL2 gl, int texture) {
      if (dirty) {
         dirty = false;
         updateTexture(gl, texture);
      }
      activateTexture(gl, texture);
   }

   abstract void updateTexture(GL2 gl, int texture);

   abstract void activateTexture(GL2 gl, int texture);

   public void releaseTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      disableTexture(gl);
   }

   abstract void disableTexture(GL2 gl);

   static int repackedDim(int n) {
      return (int) Math.ceil((float) n / 4);
   }
}
