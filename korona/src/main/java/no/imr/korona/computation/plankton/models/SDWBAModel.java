package no.imr.korona.computation.plankton.models;

import org.apache.commons.numbers.complex.Complex;

/**
 * Implementation of Stochastic Distorted Wave Born Approximation (SDWBA) for krill.
 * <p>
 * This model and reference distributions N(X, Y) is taken from:
 * David A. Demer: New target strength model indicates more krill in the Southern Ocean.
 * ICES Journal of Marine Science, 62: 25-32 (2005).
 * Stephane G. Conti, David A. Demer: Improved parametrization of the SDWBA for estimating krill target strength.
 * ICES Journal of Marine Science, 63: 928-935 (2006).
 * <p>
 * Coefficients does not match coefficients in referenced paper, since
 * the necessary complex coefficients are not listed there.
 * Instead, coefficients were calculated using matlab routines obtained from Demer and Conti.
 */
public final class SDWBAModel extends BackscatterModel {
   private double soundSpeed = 1500;
   private final double referenceVolume;
   private final double lengthToWidth;

   private SDWBAParameterSetName parameterSetName = SDWBAParameterSetName.N42;
   private SDWBAParameterSet parameterSet = getParameterSet(parameterSetName);
   private static final double L0 = 38.35E-3;

   public SDWBAModel() {
      referenceVolume = calculateReferenceVolume();
      double rSquared = referenceVolume / (L0 * Math.PI);
      lengthToWidth = L0 / Math.sqrt(rSquared);
   }

   @Override
   public double getBackscatter(double size, double frequency) {
      double k = getWaveNumber(frequency);
      double ts = calculateTS(parameterSet, k, size);
      return Math.pow(10, ts / 10);
   }

   /**
    * Calculate reduced TS. RTS(kL) = TS(kL, L) - 20*log10(L/L0).
    * Use L = L0.
    *
    * @param ka the ka
    * @return the RTS
    */
   @Override
   public double getReducedTS(double ka) {
      double L0 = parameterSet.L0;
      double a = L0 / (2 * lengthToWidth);
      double k = ka / a;
      return calculateTS(parameterSet, k, L0) - 20 * Math.log10(L0 / L0);
   }

   /**
    * {@return the volume of a krill with a given size}
    * Volume is scaled wrt reference krill in McGehee 1998.
    *
    * @param size size of the krill
    */
   @Override
   public double getBioVolume(double size) {
      return referenceVolume * Math.pow(size / parameterSet.L0, 3);
   }

   /**
    * {@return the volume of a reference length krill fattened by 40%}
    * Numbers taken from Krill31_6mmData from Demer & Conti MatLab files.
    */
   private static double calculateReferenceVolume() {
      double f = 1.4; //Fatten by 40%, Demer & Conti (2003)
      double s = 0.0338494915254237 / L0; //Scale from 33.85 to 38.35 mm as specified in paper and MatLab source.

      double totalVolume = 0;
      totalVolume += getSingleCylinderVolume(0, 0.000189517601043, 0.0338494915254237 - 0.0325311082138201, f, s);
      totalVolume += getSingleCylinderVolume(0.000189517601043, 0.000575968709257, 0.0325311082138201 - 0.0300508996088657, f, s);
      totalVolume += getSingleCylinderVolume(0.000575968709257, 0.0009970273794, 0.0300508996088657 - 0.0259639113428944, f, s);
      totalVolume += getSingleCylinderVolume(0.0009970273794, 0.001194784876141, 0.0259639113428944 - 0.0235001825293351, f, s);
      totalVolume += getSingleCylinderVolume(0.001194784876141, 0.001277183833116, 0.0235001825293351 - 0.0207645371577575, f, s);
      totalVolume += getSingleCylinderVolume(0.001277183833116, 0.001409022164276, 0.0207645371577575 - 0.0182678487614081, f, s);
      totalVolume += getSingleCylinderVolume(0.001409022164276, 0.001367822685789, 0.0182678487614081 - 0.0156228422425033, f, s);
      totalVolume += getSingleCylinderVolume(0.001367822685789, 0.001458461538462, 0.0156228422425033 - 0.0134063102998696, f, s);
      totalVolume += getSingleCylinderVolume(0.001458461538462, 0.001680938722295, 0.0134063102998696 - 0.0113380964797914, f, s);
      totalVolume += getSingleCylinderVolume(0.001680938722295, 0.001549100391134, 0.0113380964797914 - 0.00929460234680574, f, s);
      totalVolume += getSingleCylinderVolume(0.001549100391134, 0.001458461538462, 0.00929460234680574 - 0.00747358539765320, f, s);
      totalVolume += getSingleCylinderVolume(0.001458461538462, 0.001219504563233, 0.00747358539765320 - 0.00586680573663625, f, s);
      totalVolume += getSingleCylinderVolume(0.001219504563233, 0.000972307692308, 0.00586680573663625 - 0.00262028683181226, f, s);
      totalVolume += getSingleCylinderVolume(0.000972307692308, 0.000486153846154, 0.00262028683181226, f, s);

      return totalVolume;
   }

   private static double getSingleCylinderVolume(double radius1, double radius2, double length, double fatness, double scaling) {
      radius1 *= fatness;
      radius2 *= fatness;

      radius1 /= scaling;
      radius2 /= scaling;
      length /= scaling;

      return (Math.PI / 3) * length * (radius1 * radius1 + radius1 * radius2 + radius2 * radius2);
   }

   public void setParameterSetName(SDWBAParameterSetName parameterSetName) {
      this.parameterSetName = parameterSetName;
      parameterSet = getParameterSet(parameterSetName);
   }

   public SDWBAParameterSetName getParameterSetName() {
      return parameterSetName;
   }

   /**
    * Formula (10) from Conti, Demer 2006.
    *
    * @param p      a set of coefficients
    * @param k      a wave number [m^-1]
    * @param length a length [m]
    * @return the TS
    */
   private static double calculateTS(SDWBAParameterSet p, double k, double length) {
      double kL = k * length;

      Complex logTerm = logarithmicTerm(p, kL);
      double polyTerm = polynomialTerm(p, kL);
      double lengthTerm = 20 * Math.log10(length / p.L0);

      return logTerm.getReal() + polyTerm + lengthTerm;
   }

   private static double polynomialTerm(SDWBAParameterSet p, double kL) {
      return p.D * Math.pow(kL, 6)
            + p.E * Math.pow(kL, 5)
            + p.F * Math.pow(kL, 4)
            + p.G * Math.pow(kL, 3)
            + p.H * Math.pow(kL, 2)
            + p.I * kL
            + p.J;
   }

   private static Complex logarithmicTerm(SDWBAParameterSet p, double kL) {
      Complex BkL = p.B.multiply(Complex.ofCartesian(kL, 0));
      Complex log10BkL = complexLog10(BkL);
      Complex fraction = log10BkL.divide(BkL);
      Complex power = fraction.pow(p.C);
      return p.A.multiply(power);
   }

   public static Complex complexLog10(Complex c) {
      Complex cLog = c.log();
      return cLog.divide(Complex.ofCartesian(10, 0).log());
   }

   private double getWaveNumber(double frequency) {
      return 2 * Math.PI * frequency / soundSpeed;
   }

   /**
    * Sets of coefficients listed in Conti/Demer 2006.
    */
   private static SDWBAParameterSet getParameterSet(SDWBAParameterSetName name) {
      return switch (name) {
         case N114 -> new SDWBAParameterSet(
               Complex.ofCartesian(12.8525592649783, 14.8976981212302),
               Complex.ofCartesian(0.108760421979364, 0.0183977419572372),
               Complex.ofCartesian(0.736553246765406, 0.162748301811755),
               -2.15631346E-11,
               1.28056380E-08,
               -2.86317054E-06,
               2.94657329E-04,
               -1.34836415E-02,
               2.24117772E-01,
               -7.94216518E+01,
               38.35E-3,
               1.00);

         case N91 -> new SDWBAParameterSet(
               Complex.ofCartesian(7.52315404877615, 10.3006864545960),
               Complex.ofCartesian(0.106210952421554, 0.0285066239370059),
               Complex.ofCartesian(0.778361587641021, -0.0207533674614561),
               -1.28990802E-11,
               5.66626275E-09,
               -7.75476884E-07,
               2.08030645E-05,
               2.59344869E-03,
               -1.26389993E-01,
               -7.58057613E+01,
               38.35E-3,
               1.00);

         case N42 -> new SDWBAParameterSet(
               Complex.ofCartesian(1.72353848060309, 0.0672250104899089),
               Complex.ofCartesian(0.0366895293024863, 7.79702757133156e-09),
               Complex.ofCartesian(1.06493061525432, -0.122995407733648),
               -3.43679776E-11,
               2.14491847E-08,
               -5.11346110E-06,
               5.74395078E-04,
               -2.97910371E-02,
               5.71025319E-01,
               -7.36389498E+01,
               38.35E-3,
               1.00);

         case N31 -> new SDWBAParameterSet(
               Complex.ofCartesian(2.34491429117671, 0.704385409754882),
               Complex.ofCartesian(0.0414237421001590, 3.20233944297361e-09),
               Complex.ofCartesian(0.980876735873980, -0.133774190703865),
               -4.44095744E-11,
               2.80128990E-08,
               -6.74306180E-06,
               7.65628157E-04,
               -4.03208327E-02,
               7.84022568E-01,
               -7.38869780E+01,
               38.35E-3,
               1.00);

         case N155 -> new SDWBAParameterSet(
               Complex.ofCartesian(3.31708692324769, 3.44176020898030),
               Complex.ofCartesian(0.0404738088714930, 0.00522940319387602),
               Complex.ofCartesian(0.757755717918850, 0.0376171527395027),
               -1.91364527E-11,
               1.18826702E-08,
               -2.80784428E-06,
               3.11827195E-04,
               -1.59861907E-02,
               2.91485193E-01,
               -7.65454608E+01,
               38.35E-3,
               1.00
         );

         //End of Demer&Conti distributions.

         //IMR distributions:

         case N3015 -> new SDWBAParameterSet(
               Complex.ofCartesian(10.4288776339155, -17.9318090826156),
               Complex.ofCartesian(0.0979775204454693, -0.0182244323195380),
               Complex.ofCartesian(0.613483339918718, -0.304551749052857),
               -2.54458499E-12,
               1.55979051E-09,
               -3.67737782E-07,
               4.03829917E-05,
               -1.86308847E-03,
               2.93210052E-02,
               -8.34574115E+01,
               38.35E-3,
               1.00);

         case N305 -> new SDWBAParameterSet(
               Complex.ofCartesian(5.54945673132851, 17.5327908001405),
               Complex.ofCartesian(0.0877370356252193, 0.0245558097101512),
               Complex.ofCartesian(0.411628656267204, 0.445437250898885),
               1.75709162E-13,
               -6.02112180E-10,
               2.68619553E-07,
               -4.49997605E-05,
               3.24942059E-03,
               -8.54798991E-02,
               -8.73937402E+01,
               38.35E-3,
               1.00);

         case N1510 -> new SDWBAParameterSet(
               Complex.ofCartesian(20.3156201588140, 21.7596813102021),
               Complex.ofCartesian(0.120788095870287, 0.0109629147112886),
               Complex.ofCartesian(0.746331820718933, 0.319803270351786),
               -8.61509989E-12,
               5.24299955E-09,
               -1.19756257E-06,
               1.24500154E-04,
               -5.54401924E-03,
               9.31587986E-02,
               -8.19425514E+01,
               38.35E-3,
               1.00);

         case N1110 -> new SDWBAParameterSet(
               Complex.ofCartesian(11.6925980377791, -7.60786613455369),
               Complex.ofCartesian(0.0902641859047165, -8.60330955945535e-11),
               Complex.ofCartesian(0.775889399146003, -0.0806364314146719),
               -1.19164334E-11,
               7.46484220E-09,
               -1.77999124E-06,
               1.98879302E-04,
               -1.01851773E-02,
               1.97007649E-01,
               -7.70731417E+01,
               38.35E-3,
               1.00);

         case N01 -> new SDWBAParameterSet(
               Complex.ofCartesian(1.71823822293669, 0.399222106374487),
               Complex.ofCartesian(0.0354117292905677, 8.48120431348391e-09),
               Complex.ofCartesian(1.02355949220042, -0.149485469528387),
               1.49957250E-11,
               -7.76829132E-09,
               1.38622903E-06,
               -8.95489718E-05,
               1.99588738E-04,
               1.04838908E-01,
               -7.03778691E+01,
               38.35E-3,
               1.00);

         case N1515 -> new SDWBAParameterSet(
               Complex.ofCartesian(1184410.02237206, 1334829.48071225),
               Complex.ofCartesian(-45.1827464276868, -48.3643416827434),
               Complex.ofCartesian(80082.2963901306, 109762.275149103),
               -5.86066708E-11,
               3.81707065E-08,
               -9.68292003E-06,
               1.20293448E-03,
               -7.52184003E-02,
               2.16056899E+00,
               -9.56089936E+01,
               38.35E-3,
               1.00);
      };
   }

   private record SDWBAParameterSet(
         Complex A,
         Complex B,
         Complex C,
         double D,
         double E,
         double F,
         double G,
         double H,
         double I,
         double J,
         double L0,
         double sigma
   ) {
   }

   /**
    * The available parameter sets.
    */
   public enum SDWBAParameterSetName {
      N114("N(11, 4)"),
      N91("N(9, 1)"),
      N42("N(4, 2)"),
      N31("N(3, 1)"),
      N155("N(15, 5)"),
      N3015("N(30, 15)"),
      N305("N(30, 5)"),
      N1510("N(15, 10)"),
      N1110("N(11, 10)"),
      N01("N(0, 1)"),
      N1515("N(15, 15)");

      private final String id;

      SDWBAParameterSetName(String id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return id;
      }
   }
}
