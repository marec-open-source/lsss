package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.Matrix4;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Vec3;

public final class JoglTransformNode extends JoglGroupNode {
   private TRS trs = TRS.IDENTITY;

   public JoglTransformNode() {
   }

   public TRS getTRS() {
      return trs;
   }

   public void setTRS(TRS trs) {
      this.trs = trs;
   }

   public void setTranslation(Vec3 translation) {
      setTRS(trs.withTranslation(translation));
   }

   public void setRotation(Matrix3 rotation) {
      setTRS(trs.withRotation(rotation));
   }

   public void setScaling(float scaling) {
      setTRS(trs.withScaling(scaling));
   }

   @Override
   public Vec3 modelToEye(Vec3 pos) {
      pos = trs.transformPoint(pos);
      JoglGroupNode parent = getParent();
      return parent != null ? parent.modelToEye(pos) : pos;
   }

   @Override
   public Vec3 eyeToModel(Vec3 pos) {
      JoglGroupNode parent = getParent();
      pos = parent != null ? parent.eyeToModel(pos) : pos;
      pos = trs.untransformPoint(pos);
      return pos;
   }

   @Override
   protected boolean drawOpaque(GL2 gl) {
      gl.glPushMatrix();
      transform(gl);
      boolean result = super.drawOpaque(gl);
      gl.glPopMatrix();
      return result;
   }

   @Override
   protected void drawTransparent(GL2 gl) {
      if (lastDrawCompletelyOpaque()) {
         return;
      }
      gl.glPushMatrix();
      transform(gl);
      super.drawTransparent(gl);
      gl.glPopMatrix();
   }

   private void transform(GL2 gl) {
      Vec3 t = trs.translation();
      gl.glTranslatef(t.x(), t.y(), t.z());

      Matrix4 r = trs.rotation().toMatrix4();
      gl.glMultMatrixf(r.toColumnWiseArray(), 0);

      float s = trs.scaling();
      gl.glScalef(s, s, s);
   }
}
