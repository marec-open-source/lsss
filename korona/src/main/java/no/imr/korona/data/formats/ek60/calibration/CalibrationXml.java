package no.imr.korona.data.formats.ek60.calibration;

public final class CalibrationXml {
   static final String CALIBRATION = "calibration";
   static final String REF_EK80 = "refEK80";
   static final String TYPE = "type";
   static final String NAME = "name";
   static final String BEGIN = "begin";
   static final String END = "end";
   static final String ID = "id";
   static final String CHANNEL = "channel";
   static final String KHZ = "kHz";
   static final String CASE = "case";
   static final String DEFAULT = "default";

   static final String GAIN = "g";
   static final String EQUIVALENT_BEAM_ANGLE = "equivalentBeamAngle";
   static final String BEAM_WIDTH_ALONGSHIP = "beamWidthAlong";
   static final String BEAM_WIDTH_ATHWARTSHIP = "beamWidthAthwart";
   static final String ANGLE_OFFSET_ALONGSHIP = "angleOffsetAlong";
   static final String ANGLE_OFFSET_ATHWARTSHIP = "angleOffsetAthwart";
   static final String SA_CORRECTIONS = "SA";

   static final String ABSORPTION_COEFFICIENT = "abs";
   static final String SOUND_VELOCITY = "sound";

   static final String BROADBAND = "broadband";
   public static final String BROADBAND_GAIN = "g";
   public static final String BROADBAND_TRANSDUCER_IMPEDANCE = "ztde";
   public static final String BROADBAND_EQUIVALENT_BEAM_ANGLE = "eba";
   public static final String BROADBAND_ANGLE_OFFSET_ATHWARTSHIP = "atao";
   public static final String BROADBAND_ANGLE_OFFSET_ALONGSHIP = "alao";
   public static final String BROADBAND_BEAM_WIDTH_ATHWARTSHIP = "atbw";
   public static final String BROADBAND_BEAM_WIDTH_ALONGSHIP = "albw";

   static final String PROFOS_RO = "ro";
   static final String PROFOS_TAU_EFF = "tauEff";
   static final String PROFOS_TAU = "tau";
   static final String PROFOS_FREQUENCY = "frequency";
   static final String PROFOS_BEAM_WIDTH_MODE = "beamWidthMode";

   public static final String HZ = "hz";

   private CalibrationXml() {
   }
}
