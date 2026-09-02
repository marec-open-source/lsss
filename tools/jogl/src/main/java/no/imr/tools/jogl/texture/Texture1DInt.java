package no.imr.tools.jogl.texture;

import com.jogamp.opengl.GL2;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL2.*;

public final class Texture1DInt extends Texture1D<int[]> {
   public Texture1DInt() {
      super(new int[1], 1);
   }

   public void setData(int[] data) {
      setData(data, data.length);
   }

   @Override
   void updateTexture1D(GL2 gl, int[] data, int nx) {
      int type = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? GL_UNSIGNED_INT_8_8_8_8_REV : GL_UNSIGNED_INT_8_8_8_8;
      gl.glTexImage1D(GL_TEXTURE_1D, 0, 4, nx, 0, GL_BGRA, type, IntBuffer.wrap(data));
   }
}
