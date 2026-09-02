package no.imr.lsss.modules.broadband.calibrationplot;

import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.CalibrationXml;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.util.ExportRounding;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

enum CalibrationPlotParameter {
   GAIN(CalibrationXml.BROADBAND_GAIN, "Gain", Unit.DB,
         "gain", ExportRounding.db(),
         broadbandData -> broadbandData::getUncalibratedGain,
         calibration -> calibration.broadbandGain),

   TRANSDUCER_IMPEDANCE(CalibrationXml.BROADBAND_TRANSDUCER_IMPEDANCE, "Transducer impedance", Unit.DB,
         "ztde", ExportRounding.db(),
         _ -> BroadbandData::getUncalibratedTransducerImpedance,
         calibration -> calibration.broadbandTransducerImpedance),

   EQUIVALENT_BEAM_ANGLE(CalibrationXml.BROADBAND_EQUIVALENT_BEAM_ANGLE, "Equivalent beam angle", Unit.DB,
         "equivalentBeamAngle", ExportRounding.db(),
         broadbandData -> broadbandData::getUncalibratedPsi,
         calibration -> calibration.broadbandEquivalentBeamAngle),

   BEAM_WIDTH_ALONGSHIP(CalibrationXml.BROADBAND_BEAM_WIDTH_ALONGSHIP, "Beam width alongship", Unit.DEGREES,
         "beamWidthAlongship", ExportRounding.degrees(),
         broadbandData -> broadbandData::getUncalibratedAlongBeamWidth,
         calibration -> calibration.broadbandBeamWidthAlongship),

   BEAM_WIDTH_ATHWARTSHIP(CalibrationXml.BROADBAND_BEAM_WIDTH_ATHWARTSHIP, "Beam width athwartship", Unit.DEGREES,
         "beamWidthAthwartship", ExportRounding.degrees(),
         broadbandData -> broadbandData::getUncalibratedAthwartBeamWidth,
         calibration -> calibration.broadbandBeamWidthAthwartship),

   ANGLE_OFFSET_ALONGSHIP(CalibrationXml.BROADBAND_ANGLE_OFFSET_ALONGSHIP, "Angle offset alongship", Unit.DEGREES,
         "angleOffsetAlongship", ExportRounding.degrees(),
         broadbandData -> _ -> broadbandData.getUncalibratedAlongAngleOffset(),
         calibration -> calibration.broadbandAngleOffsetAlongship),

   ANGLE_OFFSET_ATHWARTSHIP(CalibrationXml.BROADBAND_ANGLE_OFFSET_ATHWARTSHIP, "Angle offset athwartship", Unit.DEGREES,
         "angleOffsetAthwartship", ExportRounding.degrees(),
         broadbandData -> _ -> broadbandData.getUncalibratedAthwartAngleOffset(),
         calibration -> calibration.broadbandAngleOffsetAthwartship),

   ABSORPTION("α", "Absorption", Unit.DB_PER_METER,
         "absorption", ExportRounding.absorption(),
         broadbandData -> broadbandData.getAbsorption()::getAbsorption,
         _ -> Optional.empty());

   final String shortName;
   final String fullName;
   final Unit unit;
   final String exportName;
   final ExportTransform exportTransform;
   final Function<BroadbandData, DoubleUnaryOperator> uncalibrated;
   final Function<ChannelCalibration, Optional<BroadbandFunction>> calibrated;

   CalibrationPlotParameter(String shortName, String fullName, Unit unit,
                            String exportName, ExportTransform exportTransform,
                            Function<BroadbandData, DoubleUnaryOperator> uncalibrated,
                            Function<ChannelCalibration, Optional<BroadbandFunction>> calibrated) {
      this.shortName = shortName;
      this.fullName = fullName;
      this.unit = unit;
      this.exportName = exportName;
      this.exportTransform = exportTransform;
      this.uncalibrated = uncalibrated;
      this.calibrated = calibrated;
   }
}
