package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import static com.jogamp.opengl.GL2.*;

public abstract class Texture3D<T> extends Texture {
   private T data;
   private int nx;
   private int ny;
   private int nz;
   private int zOffset;

   Texture3D(T data, int nx, int ny, int nz, int zOffset) {
      this.data = data;
      this.nx = nx;
      this.ny = ny;
      this.nz = nz;
      this.zOffset = zOffset;
   }

   public void setData(T data, int nx, int ny, int nz, int zOffset) {
      this.data = data;
      this.nx = nx;
      this.ny = ny;
      this.nz = nz;
      this.zOffset = zOffset;

      markDirty();
   }

   public int getRepackedZDim() {
      return repackedDim(nz);
   }

   public int getActualZDim() {
      return nz;
   }

   public int getZOffset() {
      return zOffset;
   }

   private void enableTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_3D);
      gl.glBindTexture(GL_TEXTURE_3D, getTextureID());

      gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
   }

   abstract void updateTexture3D(GL2 gl, T data, int nx, int ny, int nz);

   @Override
   void updateTexture(GL2 gl, int texture) {
      enableTexture(gl, texture);
      updateTexture3D(gl, data, nx, ny, nz);
      disableTexture(gl);
   }

   @Override
   void activateTexture(GL2 gl, int texture) {
      gl.glClientActiveTexture(texture);
      gl.glActiveTexture(texture);
      gl.glEnable(GL_TEXTURE_3D);
      gl.glBindTexture(GL_TEXTURE_3D, getTextureID());
      gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
   }

   @Override
   void disableTexture(GL2 gl) {
      gl.glBindTexture(GL_TEXTURE_3D, 0);
      gl.glDisable(GL_TEXTURE_3D);
   }
}
