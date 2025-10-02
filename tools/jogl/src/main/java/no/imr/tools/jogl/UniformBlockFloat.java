package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.Utils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class UniformBlockFloat implements JoglDisposable {
   private final String name;
   private final int bindingPoint;
   private int bufferID;
   private float[] data = Utils.EMPTY_FLOAT_ARRAY;
   private boolean dirty;

   public UniformBlockFloat(String name, int bindingPoint) {
      this.name = name;
      this.bindingPoint = bindingPoint;
   }

   public void setData(float[] data) {
      this.data = data;
      dirty = true;
   }

   public int getSize() {
      return data.length;
   }

   public void init(GL2 gl, List<JoglDisposable> disposables, Shader shader) {
      IntBuffer buffer = IntBuffer.allocate(1);
      gl.glGenBuffers(1, buffer);
      bufferID = buffer.get(0);
      int blockIndex = gl.glGetUniformBlockIndex(shader.getProgramID(), name);
      gl.glUniformBlockBinding(shader.getProgramID(), blockIndex, bindingPoint);
      dirty = true;
      disposables.add(this);
   }

   @Override
   public void dispose(GL2 gl) {
      gl.glDeleteBuffers(1, new int[]{bufferID}, 0);
      bufferID = 0;
   }

   public void bindBuffer(GL2 gl) {
      if (dirty) {
         dirty = false;
         gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferID);
         gl.glBufferData(GL_UNIFORM_BUFFER, data.length * (long) Float.BYTES, FloatBuffer.wrap(data), GL_DYNAMIC_DRAW);
         gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
      }
      gl.glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, bufferID);
   }

   public void releaseBuffer(GL2 gl) {
      gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
   }
}
