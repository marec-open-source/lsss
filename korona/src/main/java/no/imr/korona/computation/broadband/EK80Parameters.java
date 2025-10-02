package no.imr.korona.computation.broadband;

/**
 * Parameters for EK80 not found in the data files.
 */
public final class EK80Parameters {
   /**
    * Original sampling rate [Hz].
    * <p>
    * History:
    * <ul>
    * <li>This parameter was originally hardcoded to 1.5e6</li>
    * <li>In an email to Rolf 16.11.2016 Olav Langeland wrote:<br>
    * <em>"Evt. hardkodet verdi av fs kan avledes av «SampleInterval» i Parameter datagrammet og «DecimationFactor» i FilterCoefficient datagrammet."</em></li>
    * <li>So we changed to fs = DecimationFactor / SampleInterval</li>
    * <li>In data from EK80 with reduced resolution this is wrong. See email from Gavin 04.07.2018</li>
    * <li>Simrad has added a new attribute to Transceiver in XML0/Configuration: RxSampleFrequency="1500000"</li>
    * <li>So we change to fs = RxSampleFrequency if present, else 1.5e6</li>
    * <li>Use RxSampleFrequency only if &gt; 0</li>
    * </ul>
    */
   public static final float fs = 1.5e6f;

   /**
    * Wideband transceiver impedance [Ohms].
    * <p>
    * Email from Lars Nonboe Andersen 17.11.2015 04:57:
    * <pre>
    *    Jeg foreslår at dere bare bruker 5.4 K da dette er verdien for alle data fra den nye transceiveren.
    *    Alle data fra tidligere transceiver versjoner må behandles som værende fra prototype transceiver
    *    og i slike tilfeller er unøyaktigheten fra å bruke 5.4 K i forhold til f.eks. 1 K av mindre betydning.
    * </pre>
    *
    * <p>
    * Rolf says EK80 still uses rwbtrx = 1000, so then LSSS has to do so as well.
    *
    * <p>
    * NB: This might change in the future.
    */
   public static final double rwbtrx = 1000;

   /**
    * Transducer quadrant nominal impedance [Ohms].
    */
   public static final double ztrd = 75;

   private EK80Parameters() {
   }
}
