package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import static com.jogamp.opengl.GL2.*;

public abstract class Texture2D<T> extends Texture {
   private T data;
   private int nx;
   private int ny;

   Texture2D(T data, int nx, int ny) {
      this.data = data;
      this.nx = nx;
      this.ny = ny;
   }

   public void setData(T data, int nx, int ny) {
      this.data = data;
      this.nx = nx;
      this.ny = ny;

      markDirty();
   }

   public int getXDim() {
      return nx;
   }

   public int getRepackedYDim() {
      return repackedDim(ny);
   }

   public int getActualYDim() {
      return ny;
   }

   private void enableTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_2D);
      gl.glBindTexture(GL_TEXTURE_2D, getTextureID());

      gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
   }

   abstract void updateTexture2D(GL2 gl, T data, int nx, int ny);

   @Override
   void updateTexture(GL2 gl, int texture) {
      enableTexture(gl, texture);
      updateTexture2D(gl, data, nx, ny);
      disableTexture(gl);
   }

   @Override
   void activateTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_2D);
      gl.glBindTexture(GL_TEXTURE_2D, getTextureID());
      gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
   }

   @Override
   void disableTexture(GL2 gl) {
      gl.glBindTexture(GL_TEXTURE_2D, 0);
      gl.glDisable(GL_TEXTURE_2D);
   }
}
