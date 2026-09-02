package no.imr.tools.jogl.volumetric;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.JoglDisposable;
import no.imr.tools.jogl.texture.Texture3DByte;
import no.imr.tools.math.linalg.Vec3;

import java.util.List;

import static com.jogamp.opengl.GL2.*;

public abstract class VolumeDisplayer extends BaseVolumeDisplayer {
   private Texture3DByte volumeSchoolMask = new Texture3DByte();

   private boolean useSchoolMask = true;
   private boolean opaque = false;
   private float grayScaleModificationFactor = 1.0f;
   private float scaleFactor;
   private int lowerFanCutoff = 0;
   private int upperFanCutoff = 20;

   //Buffer for the last set min/max angles, in case of an automatic redraw.
   private float minTheta;
   private float maxTheta;
   private float minPhi;
   private float maxPhi;

   protected VolumeDisplayer(String vertexShaderSource, String fragmentShaderSource) {
      super(vertexShaderSource, fragmentShaderSource);
   }

   public void setUseSchoolMask(boolean useSchoolMask) {
      this.useSchoolMask = useSchoolMask;
      repaint();
   }

   public void setOpaque(boolean opaque) {
      this.opaque = opaque;
   }

   public void setVolumeSchoolMask(Texture3DByte volumeSchoolMask) {
      this.volumeSchoolMask = volumeSchoolMask;
   }

   public void setUserInteractiveMode(boolean userInteractiveMode) {
      this.userInteractiveMode = userInteractiveMode;
   }

   public boolean isDisabled() {
      return disabled;
   }

   public float getGrayScaleModificationFactor() {
      return grayScaleModificationFactor;
   }

   public void setGrayScaleModificationFactor(float grayScaleModificationFactor) {
      this.grayScaleModificationFactor = grayScaleModificationFactor;
   }

   public int getLowerFanCutoff() {
      return lowerFanCutoff;
   }

   public void setLowerFanCutoff(int lowerFanCutoff) {
      this.lowerFanCutoff = lowerFanCutoff;
   }

   public int getUpperFanCutoff() {
      return upperFanCutoff;
   }

   public void setUpperFanCutoff(int upperFanCutoff) {
      this.upperFanCutoff = upperFanCutoff;
   }

   @Override
   protected void initAllTextures(GL2 gl, List<JoglDisposable> disposables) {
      texture3D.init(gl, disposables);
      volumeSchoolMask.init(gl, disposables);
      texture1D.init(gl, disposables);
   }

   private void setShaderParams(int numSamples, int sampleOffset) {
      shader.addFloatParam("xscale", numSamples + sampleOffset);
      shader.addFloatParam("xoffset", 0.5f);
      shader.addFloatParam("yscale", numSamples + sampleOffset);
      shader.addFloatParam("yoffset", 0.0f);
      shader.addFloatParam("zscale", (numSamples + sampleOffset) * (float) Math.cos(Math.PI / 4));
      shader.addFloatParam("zoffset", -0.5f);

      shader.addFloatParam("maxR", numSamples + sampleOffset);
      shader.addFloatParam("minR", sampleOffset);

      shader.addIntParam("numSamples", numSamples);

      shader.addIntParam("opaque", opaque ? 1 : 0);
      shader.addIntParam("useSchoolMask", useSchoolMask ? 1 : 0);
      shader.addFloatParam("grayscalemodifier", grayScaleModificationFactor);
      shader.addIntParam("lowerFanCutoff", lowerFanCutoff);
      shader.addIntParam("upperFanCutoff", upperFanCutoff);

      shader.addIntParam("texture3d", 0);
      shader.addIntParam("colormap", 1);
      shader.addIntParam("schoolmask", 2);

      shader.addFloatParam("planeDist", isUserInteractiveMode() ? 0.01f : 0.001f);
   }

   public void setScaleFactor(float scaleFactor) {
      this.scaleFactor = scaleFactor;
   }

   /**
    * Sets the angles in radians.
    *
    * @param minTheta minimum theta angle [radians]
    * @param maxTheta maximum theta angle [radians]
    * @param minPhi   minimum phi angle [radians]
    * @param maxPhi   maximum phi angle [radians]
    */
   public void setAngles(float minTheta, float maxTheta, float minPhi, float maxPhi) {
      this.minTheta = minTheta;
      this.maxTheta = maxTheta;
      this.minPhi = minPhi;
      this.maxPhi = maxPhi;

      shader.addFloatParam("thetaScale", maxTheta - minTheta);
      shader.addFloatParam("thetaOffset", minTheta);
      shader.addFloatParam("phiScale", maxPhi - minPhi);
      shader.addFloatParam("phiOffset", minPhi);
   }

   public float getScaleFactor() {
      return scaleFactor;
   }

   protected void releaseTextures(GL2 gl) {
      volumeSchoolMask.releaseTexture(gl, GL_TEXTURE2);
      texture1D.releaseTexture(gl, GL_TEXTURE1);
      texture3D.releaseTexture(gl, GL_TEXTURE0);
      shader.release(gl);
   }

   protected void bindTextures(GL2 gl) {
      texture3D.bindTexture(gl, GL_TEXTURE0);
      texture1D.bindTexture(gl, GL_TEXTURE1);
      volumeSchoolMask.bindTexture(gl, GL_TEXTURE2);
      shader.use(gl);
   }

   protected Vec3 mapNocToModel(Vec3 nocCoord) {
      return nocCoord.plus(0.5f, 0.0f, -0.5f).times(scaleFactor, scaleFactor, scaleFactor / (float) Math.sqrt(2));
   }

   public Vec3 mapModelToNoc(Vec3 modelCoord) {
      return modelCoord.times(1.0f / scaleFactor, 1.0f / scaleFactor, (float) Math.sqrt(2) / scaleFactor).minus(0.5f, 0.0f, -0.5f);
   }

   @Override
   protected void draw(GL2 gl) {
      if (disabled) {
         return;
      }

      //todo: optimize
      // for now setting the shader parameters for each draw pass
      setShaderParams(texture3D.getActualZDim(), texture3D.getZOffset());
      setAngles(minTheta, maxTheta, minPhi, maxPhi);

      gl.glPushMatrix();
      gl.glScalef(scaleFactor, scaleFactor, scaleFactor / (float) Math.sqrt(2));
      gl.glTranslatef(0.5f, 0.0f, -0.5f);

      doDisplay(gl);
      gl.glPopMatrix();
   }

   protected static void drawTexturedPolygons(List<MarchingCube.Polygon> polygons, GL2 gl) {
      drawTexturedPolygons(polygons, gl, new Vec3(0.5f, 0.5f, 0.5f));
   }
}
