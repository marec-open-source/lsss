package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import java.nio.ByteBuffer;

import static com.jogamp.opengl.GL2.*;

public final class Texture2DByte extends Texture2D<byte[]> {
   public Texture2DByte() {
      super(new byte[1], 1, 1);
   }

   @Override
   void updateTexture2D(GL2 gl, byte[] data, int nx, int ny) {
      int repackedNy = repackedDim(ny);
      if (ny != repackedNy * 4) {
         data = repackData(data, nx, ny, repackedNy);
      }
      gl.glTexImage2D(GL_TEXTURE_2D, 0, 4, repackedNy, nx, 0, GL_RGBA, GL_UNSIGNED_BYTE, ByteBuffer.wrap(data));
   }

   private static byte[] repackData(byte[] data, int nx, int ny, int repackedNy) {
      byte[] newData = new byte[nx * repackedNy * 4];
      for (int i = 0; i < nx; i++) {
         int oldOffset = i * ny;
         int newOffset = i * repackedNy * 4;
         System.arraycopy(data, oldOffset, newData, newOffset, ny);
      }
      return newData;
   }
}
