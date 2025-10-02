package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.Utils;
import no.imr.tools.math.linalg.Vec3;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class VertexBufferFloat implements JoglDisposable {
   private int bufferID;
   private float[] data = Utils.EMPTY_FLOAT_ARRAY;
   private boolean dirty;

   public VertexBufferFloat() {
   }

   public void setData(float[] data) {
      this.data = data;
      dirty = true;
   }

   public void setDataFromPoints(List<Vec3> points) {
      setData(pointsToArray(points));
   }

   private static float[] pointsToArray(List<Vec3> points) {
      float[] data = new float[3 * points.size()];
      int i = 0;
      for (Vec3 point : points) {
         data[i++] = point.x();
         data[i++] = point.y();
         data[i++] = point.z();
      }
      return data;
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
      IntBuffer buffer = IntBuffer.allocate(1);
      gl.glGenBuffers(1, buffer);
      bufferID = buffer.get(0);
      dirty = true;
      disposables.add(this);
   }

   @Override
   public void dispose(GL2 gl) {
      gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
      bufferID = 0;
   }

   public void bindBuffer(GL2 gl) {
      bindBuffer(gl, 0);
   }

   public void bindBuffer(GL2 gl, int textureUnit) {
      if (dirty) {
         dirty = false;
         gl.glBindBuffer(GL_ARRAY_BUFFER, bufferID);
         gl.glBufferData(GL_ARRAY_BUFFER, data.length * (long) Float.BYTES, FloatBuffer.wrap(data), GL_DYNAMIC_DRAW);
      }
      if (textureUnit != 0) {
         gl.glActiveTexture(textureUnit);
         gl.glClientActiveTexture(textureUnit);
         gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
      } else {
         gl.glEnableClientState(GL_VERTEX_ARRAY);
      }
      gl.glBindBuffer(GL_ARRAY_BUFFER, bufferID);
      if (textureUnit != 0) {
         gl.glTexCoordPointer(3, GL_FLOAT, 0, 0);
      } else {
         gl.glVertexPointer(3, GL_FLOAT, 0, 0);
      }
   }

   public void draw(GL2 gl, int mode) {
      gl.glDrawArrays(mode, 0, data.length / 3);
      gl.glFlush();
   }

   public void releaseBuffer(GL2 gl, int textureUnit) {
      if (textureUnit != 0) {
         gl.glClientActiveTexture(textureUnit);
         gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
      } else {
         gl.glDisableClientState(GL_VERTEX_ARRAY);
      }
      gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
   }

   public void releaseBuffer(GL2 gl) {
      releaseBuffer(gl, 0);
   }
}
