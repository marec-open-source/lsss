package no.imr.tools.misc;

@FunctionalInterface
public interface FloatUnaryOperator {
   float applyAsFloat(float operand);

   static FloatUnaryOperator identity() {
      return t -> t;
   }
}
