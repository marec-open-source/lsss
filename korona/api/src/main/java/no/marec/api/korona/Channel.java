package no.marec.api.korona;

/**
 * A channel in a {@link Ping}.
 */
public interface Channel {
   /**
    * The number of this channel.
    *
    * @return the channel number
    */
   int getChannelNumber();

   /**
    * The frequency in Hz.
    *
    * @return frequency in Hz
    */
   double getFrequency();

   /**
    * The first valid sample index.
    *
    * @return the first valid sample index
    */
   int getBeginSampleIndex();

   /**
    * One past the last valid sample index.
    *
    * @return one past the last valid sample index
    */
   int getEndSampleIndex();

   /**
    * Gets (linear) s<sub>v</sub> value for a sample.
    * <p>
    * Requirement: {@link #getBeginSampleIndex()} &le; sampleIndex &lt; {@link #getEndSampleIndex()}.
    *
    * @param sampleIndex the sample index
    * @return (linear) s<sub>v</sub>
    */
   double getSv(int sampleIndex);

   /**
    * Sets (linear) s<sub>v</sub> for a sample.
    * <p>
    * Requirement: {@link #getBeginSampleIndex()} &le; sampleIndex &lt; {@link #getEndSampleIndex()}.
    *
    * @param sampleIndex the sample index
    * @param sv          (linear) s<sub>v</sub>
    */
   void setSv(int sampleIndex, double sv);

   /**
    * Gets (logarithmic) S<sub>v</sub> value for a sample.
    * <p>
    * Requirement: {@link #getBeginSampleIndex()} &le; sampleIndex &lt; {@link #getEndSampleIndex()}.
    *
    * @param sampleIndex the sample index
    * @return (logarithmic) S<sub>v</sub>
    */
   double getLogSv(int sampleIndex);

   /**
    * Sets (logarithmic) S<sub>v</sub> value for a sample.
    * <p>
    * Requirement: {@link #getBeginSampleIndex()} &le; sampleIndex &lt; {@link #getEndSampleIndex()}.
    *
    * @param sampleIndex the sample index
    * @param logSv       (logarithmic) S<sub>v</sub>
    */
   void setLogSv(int sampleIndex, double logSv);
}
