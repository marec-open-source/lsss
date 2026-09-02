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
 * Instead, coefficients were calculated using MATLAB routines obtained from Demer and Conti.
 */
public final class SDWBAModelNew extends BackscatterModel {
   private double soundSpeed = 1500;
   private final double referenceVolume;
   private final double lengthToWidth;

   private SDWBAParameterSetName parameterSetName = SDWBAParameterSetName.N0_30b;
   private SDWBAParameterSet parameterSet = getParameterSet(parameterSetName);
   private static final double L0 = 38.35e-3;

   public SDWBAModelNew() {
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
    * Numbers taken from Krill31_6mmData from Demer & Conti MATLAB files.
    */
   private static double calculateReferenceVolume() {
      double f = 1.4; //Fatten by 40%, Demer & Conti (2003)
      double s = 0.0338494915254237 / L0; //Scale from 33.85 to 38.35 mm as specified in paper and MATLAB source.

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

      double x = p.A
            + p.B / kL
            + p.C * kL
            + p.D * Math.sin(p.E * kL) * Math.exp(-p.F * kL)
            + p.G * Math.sin(p.H * kL) * Math.exp(-p.I * kL);
      return -100.0 * Math.log(x);
   }

   /*
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
   */

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
         case N0_1 -> new SDWBAParameterSet(
               2.102882044,
               1.275474547,
               -0.000646145,
               0.011337844,
               0.263137379,
               -0.01010378,
               356.0359126,
               -6.17e-05,
               0.049877645,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N0_5 -> new SDWBAParameterSet(
                  1.992634961,
                  1.336608072,
                  0.000161182,
                  0.132071317,
                  0.222501699,
                  0.026538479,
                  -0.161176819,
                  0.100344831,
                  0.067088528,
                  38.35e-3);
      */

         case N0_5b -> new SDWBAParameterSet(
               1.992323679,
               1.339177294,
               0.000163192,
               0.135618995,
               0.222777068,
               0.027949301,
               -0.166491078,
               0.101602435,
               0.067339425,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N0_10 -> new SDWBAParameterSet(
                  2.027504873,
                  1.281968434,
                  0.000137487,
                  0.134815949,
                  0.226068027,
                  0.037847814,
                  -0.179713421,
                  0.097003827,
                  0.072032588,
                  38.35e-3);
      */

         case N0_10b -> new SDWBAParameterSet(
               2.027260863,
               1.283515807,
               0.000139223,
               0.134423019,
               0.226748893,
               0.038280385,
               -0.192229867,
               0.096223447,
               0.074576911,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N0_15 -> new SDWBAParameterSet(
                  2.054620731,
                  1.243702177,
                  0.000105818,
                  0.127850467,
                  0.228270995,
                  0.041642256,
                  -0.184748687,
                  0.089858185,
                  0.074084928,
                  38.35e-3);
      */

         case N0_15b -> new SDWBAParameterSet(
               2.054848018,
               1.241298375,
               0.000104595,
               0.130507513,
               0.228432778,
               0.042418805,
               -0.18441771,
               0.091759901,
               0.073456923,
               38.35e-3);

         case N0_20b -> new SDWBAParameterSet(
               2.074915633,
               1.209412237,
               7.7001e-05,
               0.135167394,
               0.229947537,
               0.047461914,
               -0.18425409,
               0.086317355,
               0.075851748,
               38.35e-3);

         case N0_25b -> new SDWBAParameterSet(
               2.093164495,
               1.18961076,
               1.39266e-05,
               0.122930052,
               0.23102019,
               0.046425319,
               -0.186029885,
               0.077554371,
               0.077931973,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N0_30 -> new SDWBAParameterSet(
                  2.112345196,
                  1.168553684,
                  -0.00010611,
                  0.123045363,
                  0.232576354,
                  0.046209367,
                  -0.241882154,
                  0.066093162,
                  0.084491429,
                  38.35e-3);
      */

         case N0_30b -> new SDWBAParameterSet(
               2.111714911,
               1.165356438,
               -0.000101667,
               0.134849721,
               0.233779638,
               0.051668284,
               -0.242800873,
               0.071582199,
               0.087637432,
               38.35e-3);

         case N0_35b -> new SDWBAParameterSet(
               2.133040068,
               1.138539389,
               -0.000272127,
               0.133188083,
               0.2355544,
               0.053614418,
               -0.437615771,
               0.041254001,
               0.093860448,
               38.35e-3);

         case N3_1 -> new SDWBAParameterSet(
               1.964086167,
               1.35005407,
               0.000204319,
               17.52611707,
               0.213523768,
               0.03016819,
               -17.46305668,
               0.213155516,
               0.03045911,
               38.35e-3);

         case N4_2 -> new SDWBAParameterSet(
               1.961053496,
               1.357506331,
               0.000336995,
               0.193979992,
               0.231132843,
               0.027221809,
               -0.118395763,
               0.171441001,
               0.045867398,
               38.35e-3);

         case N4_5 -> new SDWBAParameterSet(
               1.989439143,
               1.320226787,
               0.000270143,
               0.163889649,
               0.228370776,
               0.033964853,
               -0.132879715,
               0.134779964,
               0.061060431,
               38.35e-3);

         case N4_10 -> new SDWBAParameterSet(
               2.030902169,
               1.272257767,
               0.000146169,
               0.14689992,
               0.228258687,
               0.039938494,
               -0.179010692,
               0.108763923,
               0.072876416,
               38.35e-3);

         case N4_15 -> new SDWBAParameterSet(
               2.057120063,
               1.239032305,
               0.00010406,
               0.133128659,
               0.229172142,
               0.042008376,
               -0.184198003,
               0.097040193,
               0.07505848,
               38.35e-3);

         case N4_30 -> new SDWBAParameterSet(
               2.114736627,
               1.166158446,
               -0.000135229,
               0.123022256,
               0.232829789,
               0.04566189,
               -0.249068738,
               0.066651598,
               0.085763198,
               38.35e-3);

         case N5_5b -> new SDWBAParameterSet(
               1.995292753,
               1.312707558,
               0.000277504,
               0.165697024,
               0.23056486,
               0.035269113,
               -0.133952453,
               0.14296827,
               0.059853831,
               38.35e-3);

         case N5_10b -> new SDWBAParameterSet(
               2.03345935,
               1.270923176,
               0.000145427,
               0.15017142,
               0.228842507,
               0.040352227,
               -0.177798547,
               0.115537592,
               0.071336095,
               38.35e-3);

         case N5_15b -> new SDWBAParameterSet(
               2.058275871,
               1.236120119,
               0.000104152,
               0.133482656,
               0.22893847,
               0.042118431,
               -0.180218328,
               0.100372692,
               0.07401963,
               38.35e-3);

         case N5_20b -> new SDWBAParameterSet(
               2.077561829,
               1.213133413,
               6.69736e-05,
               0.118600925,
               0.229794661,
               0.040579171,
               -0.167093682,
               0.092813199,
               0.074969462,
               38.35e-3);

         case N5_25b -> new SDWBAParameterSet(
               2.095804541,
               1.191392,
               -1.04457e-05,
               0.114580285,
               0.230800537,
               0.041534531,
               -0.193854833,
               0.079085645,
               0.079070852,
               38.35e-3);

         case N5_30b -> new SDWBAParameterSet(
               2.115422623,
               1.169593381,
               -0.000143541,
               0.116269102,
               0.23124665,
               0.040464477,
               -0.236118961,
               0.068462523,
               0.084107024,
               38.35e-3);

         case N5_35b -> new SDWBAParameterSet(
               2.137467011,
               1.141586164,
               -0.00032101,
               0.115724316,
               0.233917965,
               0.044222931,
               0.55986346,
               -0.032101957,
               0.093348862,
               38.35e-3);

         case N9_1 -> new SDWBAParameterSet(
               2.225748936,
               0.992436271,
               -0.000248445,
               0.084781815,
               0.261566035,
               0.006200816,
               -2295.825851,
               1.61e-05,
               0.072869431,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N11_4 -> new SDWBAParameterSet(
                  2.111960812,
                  1.184717307,
                  -4.36e-05,
                  0.092193935,
                  0.259607087,
                  0.018015188,
                  -0.240500864,
                  0.117678117,
                  0.078381608,
                  38.35e-3);
      */

         case N11_4b -> new SDWBAParameterSet(
               2.1123403794,
               1.1846416069,
               -0.0000455629,
               0.0960012373,
               0.2600986448,
               0.0186685734,
               -0.2301776589,
               0.1266269840,
               0.0759528734,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N11_10 -> new SDWBAParameterSet(
                  2.064027274,
                  1.22647046,
                  0.000103274,
                  0.141594086,
                  0.23407087,
                  0.04003089,
                  -0.178445587,
                  0.119621996,
                  0.072507263,
                  38.35e-3);
      */

         case N11_10b -> new SDWBAParameterSet(
               2.0641069846,
               1.2291150549,
               0.0001026657,
               0.1445273453,
               0.2337524176,
               0.0403962708,
               -0.1855452137,
               0.1207872718,
               0.0717778429,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N11_15 -> new SDWBAParameterSet(
                  2.073535088,
                  1.215497873,
                  8.22e-05,
                  0.13706674,
                  0.231272026,
                  0.042273884,
                  -0.178439923,
                  0.108825046,
                  0.074755613,
                  38.35e-3);
      */

         case N11_15b -> new SDWBAParameterSet(
               2.0735566930,
               1.2141734203,
               0.0000823102,
               0.1427241417,
               0.2316327598,
               0.0443480194,
               -0.1858652464,
               0.1118896500,
               0.0749676684,
               38.35e-3);

         // Demer/Renfree parameters containing a small error according to Demer:
         case N11_20b -> new SDWBAParameterSet(
               2.0873452092,
               1.1950727333,
               0.0000352232,
               0.1349270064,
               0.2309108968,
               0.0436907885,
               -0.1812920957,
               0.0987788645,
               0.0770174635,
               38.35e-3);

         case N11_25b -> new SDWBAParameterSet(
               2.1039527998,
               1.1791887616,
               -0.0000670541,
               0.1211526656,
               0.2319994771,
               0.0420677954,
               -0.2032497130,
               0.0861776906,
               0.0812501016,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N11_30 -> new SDWBAParameterSet(
                  2.123770659,
                  1.156260108,
                  -0.00022233,
                  0.12294417,
                  0.233591398,
                  0.044998609,
                  -0.280208876,
                  0.063836268,
                  0.089324621,
                  38.35e-3);
      */

         case N11_30b -> new SDWBAParameterSet(
               2.1246239883,
               1.1593667835,
               -0.0002282645,
               0.1140274855,
               0.2329411554,
               0.0414256038,
               -0.2911912024,
               0.0587926415,
               0.0888150056,
               38.35e-3);

         case N11_35b -> new SDWBAParameterSet(
               2.1449969649,
               1.1371723938,
               -0.0004000717,
               0.1095867948,
               0.2330938860,
               0.0418241132,
               0.7178417687,
               -0.0242820419,
               0.0932141170,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N15_5 ->
            new SDWBAParameterSet(
                  2.18398641,
                  1.102495671,
                  -0.000354035,
                  0.09328208,
                  0.267703837,
                  0.023882082,
                  -0.337277351,
                  0.122338622,
                  0.093383763,
                  38.35e-3);
      */

         case N15_5b -> new SDWBAParameterSet(
               2.183372197,
               1.108954091,
               -0.000350669,
               0.085843444,
               0.267323125,
               0.022909029,
               -0.329791775,
               0.121391079,
               0.093673174,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N15_10 -> new SDWBAParameterSet(
                  2.097576228,
                  1.185794049,
                  3.60e-05,
                  0.118988319,
                  0.239990992,
                  0.036151865,
                  -0.170417529,
                  0.120181768,
                  0.07080834,
                  38.35e-3);
      */

         case N15_10b -> new SDWBAParameterSet(
               2.097509002,
               1.186711187,
               3.63031e-05,
               0.122267793,
               0.240718947,
               0.037461746,
               -0.173555994,
               0.121614802,
               0.070887954,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N15_15 -> new SDWBAParameterSet(
                  2.089537228,
                  1.193686301,
                  5.46e-05,
                  0.135834271,
                  0.2329429,
                  0.042107144,
                  -0.172315954,
                  0.114984597,
                  0.073925533,
                  38.35e-3);
      */

         case N15_15b -> new SDWBAParameterSet(
               2.0896659299,
               1.1940221876,
               0.0000536502,
               0.1281463253,
               0.2331830396,
               0.0413617649,
               -0.1643391952,
               0.1163581730,
               0.0736849037,
               38.35e-3);

         case N15_20b -> new SDWBAParameterSet(
               2.097178893,
               1.187175502,
               -2.2546e-07,
               0.125569602,
               0.232253059,
               0.042357986,
               -0.173305133,
               0.10143215,
               0.077983127,
               38.35e-3);

         case N15_25b -> new SDWBAParameterSet(
               2.112511461,
               1.165919826,
               -0.000126596,
               0.128313685,
               0.232691718,
               0.045283597,
               -0.201681961,
               0.08951565,
               0.083568555,
               38.35e-3);

      /* Demer/Renfree parameters containing a small error according to Demer:
            case N15_30 ->
            new SDWBAParameterSet(
                  2.131582774,
                  1.147450712,
                  -0.000292898,
                  0.122639432,
                  0.234220398,
                  0.044740882,
                  -0.311138002,
                  0.059931105,
                  0.091821472,
                  38.35e-3);
      */

         case N15_30b -> new SDWBAParameterSet(
               2.1313388978,
               1.1446434443,
               -0.0002912341,
               0.1256943906,
               0.2348283913,
               0.0452827662,
               -0.3016185709,
               0.0626409363,
               0.0924685544,
               38.35e-3);

         case N15_35b -> new SDWBAParameterSet(
               2.149379065,
               1.129240209,
               -0.000451896,
               0.118903657,
               0.234463909,
               0.043753127,
               0.53271776,
               -0.037482035,
               0.095722793,
               38.35e-3);

         case N30_5 -> new SDWBAParameterSet(
               2.264009528,
               0.962959609,
               -0.000317397,
               0.039269755,
               0.246038288,
               0.016337241,
               -0.033000952,
               0.164472487,
               0.014784951,
               38.35e-3);

         case N30_10 -> new SDWBAParameterSet(
               2.226301932,
               1.025261537,
               -0.000288283,
               0.047798614,
               0.25279652,
               0.016027738,
               -0.053607379,
               0.162127562,
               0.026312067,
               38.35e-3);

         case N30_15 -> new SDWBAParameterSet(
               2.176976415,
               1.076670674,
               -0.000256833,
               0.09209969,
               0.246293311,
               0.031902417,
               -0.079311261,
               0.14140029,
               0.047441441,
               38.35e-3);

         case N30_30 -> new SDWBAParameterSet(
               2.17128986,
               1.09983031,
               -0.000655509,
               0.123977609,
               0.237250491,
               0.046006525,
               -0.426743731,
               0.047673044,
               0.098268911,
               38.35e-3);

      /*
            case N91 -> new SDWBAParameterSet(
                  new Complex(7.52315404877615, 10.3006864545960),
                  new Complex(0.106210952421554, 0.0285066239370059),
                  new Complex(0.778361587641021, -0.0207533674614561),
                  -1.28990802e-11,
                  5.66626275e-09,
                  -7.75476884e-07,
                  2.08030645e-05,
                  2.59344869e-03,
                  -1.26389993e-01,
                  -7.58057613e+01,
                  38.35e-3,
                  1.00);
       */
      };
   }

   private record SDWBAParameterSet(
         double A,
         double B,
         double C,
         double D,
         double E,
         double F,
         double G,
         double H,
         double I,
         //double J,
         double L0
         //double sigma
   ) {
   }

   /**
    * The available parameter sets.
    */
   public enum SDWBAParameterSetName {
      N0_1("N(0, 1)"),
      // N0_5("N(0, 5)"),
      N0_5b("N(0, 5)"),
      // N0_10("N(0, 10)"),
      N0_10b("N(0, 10)"),
      // N0_15("N(0, 15)"),
      N0_15b("N(0, 15)"),
      N0_20b("N(0, 20)"),
      N0_25b("N(0, 25)"),
      // N0_30("N(0, 30)"),
      N0_30b("N(0, 30)"),
      N0_35b("N(0, 35)"),
      N3_1("N(3, 1)"),
      N4_2("N(4, 2)"),
      N4_5("N(4, 5)"),
      N4_10("N(4, 10)"),
      N4_15("N(4, 15)"),
      N4_30("N(4, 30)"),
      N5_5b("N(5, 5)"),
      N5_10b("N(5, 10)"),
      N5_15b("N(5, 15)"),
      N5_20b("N(5, 20)"),
      N5_25b("N(5, 25)"),
      N5_30b("N(5, 30)"),
      N5_35b("N(5, 35)"),
      N9_1("N(9, 1)"),
      // N11_4("N(11, 4)"),
      N11_4b("N(11, 4)"),
      // N11_10("N(11, 10)"),
      N11_10b("N(11, 10)"),
      // N11_15("N(11, 15)"),
      N11_15b("N(11, 15)"),
      N11_20b("N(11, 20)"),
      N11_25b("N(11, 25)"),
      // N11_30("N(11, 30)"),
      N11_30b("N(11, 30)"),
      N11_35b("N(11, 35)"),
      // N15_5("N(15, 5)"),
      N15_5b("N(15, 5)"),
      // N15_10("N(15, 10)"),
      N15_10b("N(15, 10)"),
      // N15_15("N(15, 15)"),
      N15_15b("N(15, 15)"),
      N15_20b("N(15, 20)"),
      N15_25b("N(15, 25)"),
      // N15_30("N(15, 30)"),
      N15_30b("N(15, 30)"),
      N15_35b("N(15, 35)"),
      N30_5("N(30, 5)"),
      N30_10("N(30, 10)"),
      N30_15("N(30, 15)"),
      N30_30("N(30, 30)");

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
