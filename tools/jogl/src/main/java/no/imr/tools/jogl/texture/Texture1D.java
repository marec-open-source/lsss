package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import static com.jogamp.opengl.GL2.*;

public abstract class Texture1D<T> extends Texture {
   private T data;
   private int nx;

   Texture1D(T data, int nx) {
      this.data = data;
      this.nx = nx;
   }

   public void setData(T data, int nx) {
      this.data = data;
      this.nx = nx;

      markDirty();
   }

   private void enableTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_1D);
      gl.glBindTexture(GL_TEXTURE_1D, getTextureID());

      gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
   }

   abstract void updateTexture3D(GL2 gl, T data, int nx);

   @Override
   void updateTexture(GL2 gl, int texture) {
      enableTexture(gl, texture);
      updateTexture3D(gl, data, nx);
      disableTexture(gl);
   }

   @Override
   void activateTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_1D);
      gl.glBindTexture(GL_TEXTURE_1D, getTextureID());
      gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
   }

   @Override
   void disableTexture(GL2 gl) {
      gl.glBindTexture(GL_TEXTURE_1D, 0);
      gl.glDisable(GL_TEXTURE_1D);
   }
}
