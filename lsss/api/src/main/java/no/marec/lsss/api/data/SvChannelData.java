package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

/**
 * Sample data containing sv and angles.
 */
@DoNotImplement
public interface SvChannelData extends ChannelData {
   /**
    * {@return the frequency in Hz}
    */
   float getFrequency();

   /**
    * {@return the sample data as sv}
    * <p>
    * The sv values are linear values that include the factor
    * <code>4 π 1852<sup>2</sup></code>.
    * Conversion to logarithmic values can be done by
    * {@link no.marec.lsss.api.util.LsssUtils#svToLogSv(double)}.
    */
   float[] getSv();

   /**
    * {@return the alongship angle for a given sample}
    *
    * @param sampleIndex a sample index
    */
   float getMechanicalAlongAngle(int sampleIndex);

   /**
    * {@return the athwartship angle for a given sample}
    *
    * @param sampleIndex a sample index
    */
   float getMechanicalAthwartAngle(int sampleIndex);
}
