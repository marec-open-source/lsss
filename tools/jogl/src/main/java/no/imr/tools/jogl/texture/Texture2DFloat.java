package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2.*;

public final class Texture2DFloat extends Texture2D<float[]> {
   public Texture2DFloat() {
      super(new float[1], 1, 1);
   }

   @Override
   void updateTexture1D(GL2 gl, float[] data, int nx, int ny) {
      int repackedNy = repackedDim(ny);
      if (ny != repackedNy * 4) {
         data = repackData(data, nx, ny, repackedNy);
      }
      gl.glTexImage2D(GL_TEXTURE_2D, 0, 4, repackedNy, nx, 0, GL_RGBA, GL_FLOAT, FloatBuffer.wrap(data));
   }

   private static float[] repackData(float[] data, int nx, int ny, int repackedNy) {
      float[] newData = new float[nx * repackedNy * 4];
      for (int i = 0; i < nx; i++) {
         int oldOffset = i * ny;
         int newOffset = i * repackedNy * 4;
         System.arraycopy(data, oldOffset, newData, newOffset, ny);
      }
      return newData;
   }
}
