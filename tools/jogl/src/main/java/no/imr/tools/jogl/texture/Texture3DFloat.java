package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2.*;

public final class Texture3DFloat extends Texture3D<float[]> {
   public Texture3DFloat() {
      super(new float[1], 1, 1, 1, 0);
   }

   @Override
   void updateTexture3D(GL2 gl, float[] data, int nx, int ny, int nz) {
      int repackedNz = repackedDim(nz);
      if (nz != repackedNz * 4) {
         data = repackData(data, nx, ny, nz, repackedNz);
      }
      gl.glTexImage3D(GL_TEXTURE_3D, 0, 4, repackedNz, ny, nx, 0, GL_RGBA, GL_FLOAT, FloatBuffer.wrap(data));
   }

   private static float[] repackData(float[] data, int nx, int ny, int nz, int repackedNz) {
      float[] newData = new float[nx * ny * repackedNz * 4];
      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < ny; j++) {
            int oldOffset = j * nz + i * ny * nz;
            int newOffset = j * repackedNz * 4 + i * ny * repackedNz * 4;
            System.arraycopy(data, oldOffset, newData, newOffset, nz);
         }
      }
      return newData;
   }
}
