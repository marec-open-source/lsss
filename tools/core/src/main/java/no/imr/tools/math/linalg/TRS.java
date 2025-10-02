package no.imr.tools.math.linalg;

/**
 * A transformation consisting of a translation, a rotation and a scaling.
 */
public record TRS(Vec3 translation, Matrix3 rotation, float scaling) implements Transform {

   public static final TRS IDENTITY = new TRS(Vec3.ZERO, Matrix3.IDENTITY, 1);

   public TRS(Vec3 translation) {
      this(translation, Matrix3.IDENTITY, 1);
   }

   public TRS(Vec3 translation, Matrix3 rotation) {
      this(translation, rotation, 1);
   }

   public TRS withTranslation(Vec3 translation) {
      return new TRS(translation, rotation, scaling);
   }

   public TRS withRotation(Matrix3 rotation) {
      return new TRS(translation, rotation, scaling);
   }

   public TRS withScaling(float scaling) {
      return new TRS(translation, rotation, scaling);
   }

   public Matrix4 toMatrix4() {
      Matrix4 t = Matrix4.createTranslation(translation);
      Matrix4 r = rotation.toMatrix4();
      Matrix4 s = Matrix4.createScaling(new Vec3(scaling, scaling, scaling));
      return t.multiply(r).multiply(s);
   }

   public TRS translate(Vec3 deltaTranslation) {
      Vec3 t = translation.plus(deltaTranslation);
      return new TRS(t, rotation, scaling);
   }

   public TRS rotate(Matrix3 deltaRotation, Vec3 center) {
      Matrix3 r = deltaRotation.multiply(rotation).normalize();

      // translation += (I - deltaRotation) * (center - translation)
      Vec3 dt = Matrix3.IDENTITY.minus(deltaRotation).multiply(center.minus(translation));
      Vec3 t = translation.plus(dt);
      return new TRS(t, r, scaling);
   }

   public TRS scale(float deltaScaling, Vec3 center) {
      Vec3 t = translation.plus(center.minus(translation).times(1 - deltaScaling));
      float s = scaling * deltaScaling;
      return new TRS(t, rotation, s);
   }

   @Override
   public Vec3 transformPoint(Vec3 point) {
      return rotation.multiply(point.times(scaling)).plus(translation);
   }

   public Vec3 transformPoint(Vec2 point) {
      return rotation.multiply(point.times(scaling)).plus(translation);
   }

   public float transformPointGetX(Vec3 point) {
      return rotation.multiplyGetX(point) * scaling + translation.x();
   }

   public float transformPointGetY(Vec3 point) {
      return rotation.multiplyGetY(point) * scaling + translation.y();
   }

   public float transformPointGetZ(Vec3 point) {
      return rotation.multiplyGetZ(point) * scaling + translation.z();
   }

   public Vec3 untransformPoint(Vec3 point) {
      return rotation.transpose().multiply(point.minus(translation)).times(1 / scaling);
   }

   public TRS multiply(TRS trs) {
      Vec3 t = translation.plus(rotation.multiply(trs.translation.times(scaling)));
      Matrix3 r = rotation.multiply(trs.rotation).normalize();
      float s = scaling * trs.scaling;
      return new TRS(t, r, s);
   }

   public TRS divide(TRS trs) {
      return multiply(trs.inverse());
   }

   public TRS inverse() {
      float s = 1 / scaling;
      Matrix3 r = rotation.transpose();
      Vec3 t = r.multiply(translation.times(-s));
      return new TRS(t, r, s);
   }
}
