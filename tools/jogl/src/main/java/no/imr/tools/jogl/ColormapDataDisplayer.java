package no.imr.tools.jogl;

import com.jogamp.opengl.GL2;
import no.imr.tools.ResourceUtils;
import no.imr.tools.jogl.texture.Texture1DInt;
import no.imr.tools.jogl.texture.Texture2D;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public final class ColormapDataDisplayer {
   private final Texture2D<?> dataTexture;
   private final Texture1DInt colormapTexture;
   private final Shader shader = new Shader();

   public ColormapDataDisplayer(Texture2D<?> dataTexture, Texture1DInt colormapTexture) {
      this.dataTexture = dataTexture;
      this.colormapTexture = colormapTexture;
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
      shader.init(gl, disposables, "", getFragmentSource());

      shader.addIntParam("data", 0);
      shader.addIntParam("colormap", 1);

      dataTexture.init(gl, disposables);
      colormapTexture.init(gl, disposables);
   }

   static String getFragmentSource() {
      return ResourceUtils.getString("no/imr/tools/resources/jogl/ColormapData2D.frag");
   }

   private void release(GL2 gl) {
      dataTexture.releaseTexture(gl, GL_TEXTURE0);
      colormapTexture.releaseTexture(gl, GL_TEXTURE1);
      shader.release(gl);
   }

   private void bind(GL2 gl) {
      dataTexture.bindTexture(gl, GL_TEXTURE0);
      colormapTexture.bindTexture(gl, GL_TEXTURE1);
      shader.use(gl);
   }

   public void draw(GL2 gl, JoglDrawable drawable) {
      shader.addIntParam("actualNy", dataTexture.getActualYDim());
      bind(gl);
      drawable.draw(gl);
      release(gl);
   }
}
