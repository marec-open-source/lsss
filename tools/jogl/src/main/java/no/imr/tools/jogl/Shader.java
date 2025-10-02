package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LogOnce;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jogamp.opengl.GL2.*;
import static com.jogamp.opengl.GL3.GL_GEOMETRY_SHADER;

public final class Shader implements JoglDisposable {
   private final Map<String, ShaderParameter> parameters = new LinkedHashMap<>();
   private int programID;
   private List<Integer> shaderIds = List.of();

   public Shader() {
   }

   public void init(GL2 gl, List<JoglDisposable> disposables, String vertexShaderSource, String fragmentShaderSource) {
      init(gl, disposables, vertexShaderSource, "", fragmentShaderSource);
   }

   public void init(GL2 gl, List<JoglDisposable> disposables, String vertexShaderSource, String geometryShaderSource, String fragmentShaderSource) {
      programID = gl.glCreateProgram();

      List<Integer> newShaderIds = new ArrayList<>();
      try {
         if (!vertexShaderSource.isEmpty()) {
            int shaderId = compileAndAttach(gl, GL_VERTEX_SHADER, vertexShaderSource);
            newShaderIds.add(shaderId);
         }
         if (!geometryShaderSource.isEmpty()) {
            int shaderId = compileAndAttach(gl, GL_GEOMETRY_SHADER, geometryShaderSource);
            newShaderIds.add(shaderId);
         }
         if (!fragmentShaderSource.isEmpty()) {
            int shaderId = compileAndAttach(gl, GL_FRAGMENT_SHADER, fragmentShaderSource);
            newShaderIds.add(shaderId);
         }
         linkProgram(gl);
      } catch (ParseException e) {
         Log.global.warning(e.getMessage());
      }
      shaderIds = List.copyOf(newShaderIds);
      disposables.add(this);
   }

   private void linkProgram(GL2 gl) throws ParseException {
      IntBuffer buffer = IntBuffer.allocate(1);
      gl.glLinkProgram(programID);
      gl.glGetProgramiv(programID, GL_LINK_STATUS, buffer);
      if (buffer.get(0) == 0) {
         throw new ParseException("Shader does not link. Info log:\n" + getInfoLog(gl, programID), 0);
      }
   }

   private int compileAndAttach(GL2 gl, int type, String source) throws ParseException {
      int shaderID = gl.glCreateShader(type);
      gl.glShaderSource(shaderID, 1, new String[]{source}, new int[]{source.length()}, 0);
      gl.glCompileShader(shaderID);
      IntBuffer buffer = IntBuffer.allocate(1);
      gl.glGetObjectParameterivARB(shaderID, GL_COMPILE_STATUS, buffer);
      if (buffer.get(0) == 0) {
         throw new ParseException("Shader does not compile. Info log:\n" + getInfoLog(gl, shaderID), 0);
      }
      gl.glAttachShader(programID, shaderID);
      gl.glGetShaderiv(shaderID, GL_COMPILE_STATUS, buffer);
      if (buffer.get(0) == 0) {
         throw new ParseException("Shader does not compile. Info log:\n" + getInfoLog(gl, shaderID), 0);
      }
      return shaderID;
   }

   @Override
   public void dispose(GL2 gl) {
      parameters.clear();
      gl.glDeleteProgram(programID);
      programID = 0;
      shaderIds.forEach(gl::glDeleteShader);
      shaderIds = List.of();
   }

   private static String getInfoLog(GL2 gl, int id) {
      IntBuffer infoLength = IntBuffer.allocate(1);
      gl.glGetObjectParameterivARB(id, GL_OBJECT_INFO_LOG_LENGTH_ARB, infoLength);
      int length = infoLength.get(0);
      if (length <= 1) {
         return "";
      }
      IntBuffer charsWritten = IntBuffer.allocate(1);
      ByteBuffer infoLog = ByteBuffer.allocate(length);
      gl.glGetInfoLogARB(id, length, charsWritten, infoLog);
      infoLog.rewind();
      return "GLSL Validation >> " + new String(infoLog.array(), Utils.UTF_8);
   }

   public void addIntParam(String name, int value) {
      setParameter(name, ShaderParameterValue.of(value));
   }

   public void addFloatParam(String name, float value) {
      setParameter(name, ShaderParameterValue.of(value));
   }

   public void bindFloatParam(GL2 gl, String name, float value) {
      ShaderParameter parameter = setParameter(name, ShaderParameterValue.of(value));
      bind(gl, parameter);
   }

   private ShaderParameter setParameter(String name, ShaderParameterValue value) {
      ShaderParameter parameter = parameters.get(name);
      if (parameter == null) {
         parameter = new ShaderParameter(name, value);
         parameters.put(name, parameter);
      } else {
         parameter.value = value;
      }
      return parameter;
   }

   public int getProgramID() {
      return programID;
   }

   public void use(GL2 gl) {
      gl.glUseProgram(programID);
      parameters.values().forEach(parameter -> {
         bind(gl, parameter);
      });
   }

   private void bind(GL2 gl, ShaderParameter parameter) {
      int location = parameter.location;
      if (location == -1) {
         location = gl.glGetUniformLocation(programID, parameter.name);
         if (location == -1) {
            LogOnce.warning("No location for shader parameter " + parameter.name, this);
            return;
         }
         parameter.location = location;
      }
      parameter.value.bind(gl, location);
   }

   public void release(GL2 gl) {
      gl.glUseProgram(0);
   }

   private static final class ShaderParameter {
      private final String name;
      private int location = -1;
      private ShaderParameterValue value;

      private ShaderParameter(String name, ShaderParameterValue value) {
         this.name = name;
         this.value = value;
      }
   }

   @FunctionalInterface
   private interface ShaderParameterValue {
      void bind(GL2 gl, int location);

      static ShaderParameterValue of(int value) {
         return (gl, location) -> {
            gl.glUniform1i(location, value);
         };
      }

      static ShaderParameterValue of(float value) {
         return (gl, location) -> {
            gl.glUniform1f(location, value);
         };
      }
   }
}
