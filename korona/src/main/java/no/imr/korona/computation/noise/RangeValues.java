package no.imr.korona.computation.noise;

public interface RangeValues {
   boolean hasBottomRange();

   float getAboveFirstBottomRange();

   float getBelowFirstBottomRange();

   float getChannelBottomRange();
}
