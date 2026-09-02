package no.imr.lsss.modules.echogram;

import com.google.common.base.Joiner;
import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolParameters;
import no.imr.korona.region.storing.StoringIntervalConfig;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.QualityEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.lsss.modules.schoolparameter.SchoolParameterModule;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.MathUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.range.FloatRange;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tooltip generator for echogram module.
 */
final class ToolTipGenerator implements ParameterContainer {
   private final BooleanParameter showHighResTime = new BooleanParameter(
         new Name("HighResTime", "High resolution time"),
         true);

   private final BooleanParameter showFileName = new BooleanParameter(
         new Name("FileName", "File name"),
         true);

   private final BooleanParameter showDepth = new BooleanParameter(
         new Name("Depth", "Depth"),
         true);

   private final BooleanParameter showBottomDepth = new BooleanParameter(
         new Name("BottomDepth", "Bottom depth"),
         true);

   private final BooleanParameter showPingNumber = new BooleanParameter(
         new Name("PingNumber", "Ping number"),
         true);

   private final BooleanParameter showVesselDistance = new BooleanParameter(
         new Name("VesselDistance", "Vessel distance"),
         true);

   private final BooleanParameter showVesselSpeed = new BooleanParameter(
         new Name("VesselSpeed", "Vessel speed"),
         true);

   private final BooleanParameter showSampleNumber = new BooleanParameter(
         new Name("SampleNumber", "Sample number"),
         false);

   private final BooleanParameter showSvValue = new BooleanParameter(
         new Name("Sv", "Sv"),
         true);

   private final BooleanParameter showTSUValue = new BooleanParameter(
         new Name("TSU", "TSU"),
         true,
         "TS uncorrected");

   private final BooleanParameter showTSCValue = new BooleanParameter(
         new Name("TSC", "TSC"),
         true,
         "TS corrected");

   private final BooleanParameter showPulseCompression = new BooleanParameter(
         new Name("PulseCompression", "Pulse compression"),
         false,
         "Average pulse compressed signal");

   private final BooleanParameter showAlongAngle = new BooleanParameter(
         new Name("AlongAngle", "Along angle"),
         true);

   private final BooleanParameter showAthwartAngle = new BooleanParameter(
         new Name("AthwartAngle", "Athwart angle"),
         true);

   private final BooleanParameter showSvThresholds = new BooleanParameter(
         new Name("SvThresholds", "Sv thresholds"),
         true);

   private final BooleanParameter showRegionLabels = new BooleanParameter(
         new Name("RegionLabels", "Region labels"),
         true);

   private final BooleanParameter showObjectNumber = new BooleanParameter(
         new Name("ObjectNumber", "Object number"),
         true,
         "Object number in database");

   private final BooleanParameter showCategory = new BooleanParameter(
         new Name("Category", "Categories"),
         true);

   private final BooleanParameter showPlankton = new BooleanParameter(
         new Name("Plankton", "Plankton"),
         true);

   private final BooleanParameter showScatter = new BooleanParameter(
         new Name("Scatter", "Scatter"),
         false);

   private final BooleanParameter showSchoolParameters = new BooleanParameter(
         new Name("SchoolParameters", "School parameters"),
         false);

   private final BooleanParameter showStoringSettings = new BooleanParameter(
         new Name("StoringSettings", "Storing settings"),
         false,
         "For database storing");

   private final BooleanParameter showNmea = new BooleanParameter(
         new Name("NMEA", "NMEA"),
         false,
         "NMEA datagrams");

   private final LSSS lsss;
   private final EchogramModule echogramModule;
   private @Nullable EchogramRectangle selectionRectangle;

   ToolTipGenerator(EchogramModule echogramModule) {
      lsss = echogramModule.getLSSS();
      this.echogramModule = echogramModule;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            showHighResTime,
            showFileName,
            showDepth,
            showBottomDepth,
            showPingNumber,
            showVesselDistance,
            showVesselSpeed,
            showSampleNumber,
            showSvValue,
            showTSUValue,
            showTSCValue,
            showPulseCompression,
            showAlongAngle,
            showAthwartAngle,
            showSvThresholds,
            showRegionLabels,
            showObjectNumber,
            showCategory,
            showPlankton,
            showScatter,
            showSchoolParameters,
            showStoringSettings,
            showNmea
      );
   }

   void setSelectionRectangle(@Nullable EchogramRectangle selectionRectangle) {
      this.selectionRectangle = selectionRectangle;
   }

   @Nullable String getToolTip(Point point) {
      PingIndex pingIndex = lsss.getInterpretationSettings().getPingSettings().xToContainingPingIndex(point.x);
      if (pingIndex == null) {
         return null;
      }
      Ping ping = lsss.getInterpretationSettings().getDataFileSet().getPing(pingIndex);

      float depth = echogramModule.getZSettings().yToDepth(point.getY(), pingIndex);
      float z = echogramModule.getZSettings().yToZ(point.getY());
      Region region = lsss.getRegionManager().getRegion(new EchogramPoint(pingIndex, depth));
      School school = region instanceof School s ? s : null;

      ChannelData channelData = ping.getChannelData(lsss.getInterpretationSettings().getChannel());
      PowerData powerData;
      int sampleIndex;
      if (channelData != null) {
         powerData = channelData.getPowerData();
         sampleIndex = powerData.depthToContainingSampleIndex(depth);
         if (sampleIndex >= powerData.getCount()) {
            sampleIndex = -1;
         }
      } else {
         powerData = null;
         sampleIndex = -1;
      }

      StringBuilder toolTip = new StringBuilder("<html><table cellpadding=0 cellspacing=0>");

      addRowCol2(toolTip, TimeUtils.JAVA_UTIL_DATE_FORMATTER.format(pingIndex.getInstant()));

      if (showHighResTime.getBooleanValue()) {
         String time = pingIndex.getInstant().toString();
         addRowCol2(toolTip, time);
      }

      if (showFileName.getBooleanValue()) {
         String fileName = lsss.getInterpretationSettings().getDataFileSet().getDataFile(pingIndex).getSegmentHandle().getDisplayName();
         addRowCol2(toolTip, fileName);
      }

      String verticalSpace = "<tr style='font-size: 0.5em'></tr>";
      toolTip.append(verticalSpace);

      EchogramRectangle selectionRectangle = this.selectionRectangle;
      if (selectionRectangle != null) {
         addRowCol2(toolTip, "Selection rectangle:");
         PingRange pingRange = selectionRectangle.pingRange();
         double vesselDistanceMeter = KoronaUtils.nmiToMeter(pingRange.getVesselDistance());
         addRowItem(toolTip, "Horizontal [m]", "(" + pingRange.getPingCount() + " pings, "
               + pingRange.getDurationString() + ")   "
               + Utils.format("%.2f", vesselDistanceMeter));
         float dz = selectionRectangle.zRange().getSize();
         addRowItem(toolTip, "Vertical [m]", Utils.format("%.2f", dz));
         addRowItem(toolTip, "Diagonal [m]", Utils.format("%.2f", MathUtils.hypot(vesselDistanceMeter, dz)));

         toolTip.append(verticalSpace);
      }

      if (showDepth.getBooleanValue()) {
         float physicalDepth = lsss.getInterpretationSettings().getDataFileSet().getDataConfiguration().depthToPhysicalDepth(depth);
         addRow(toolTip, "Depth [m]", Utils.format("%.2f", physicalDepth));
      }

      if (showBottomDepth.getBooleanValue()) {
         double bottomPhysicalDepth;
         DataConfiguration dataConfiguration = lsss.getInterpretationSettings().getDataFileSet().getDataConfiguration();
         if (dataConfiguration.isSeabedMounted()) {
            bottomPhysicalDepth = dataConfiguration.getSeabedMountedSeabedPhysicalDepth();
         } else {
            bottomPhysicalDepth = lsss.getInterpretationSettings().getDataFileSet().getBot0Datagram(pingIndex).getChannelDepths()[lsss.getInterpretationSettings().getChannel() - 1];
         }
         addRow(toolTip, "Bottom [m]", Utils.format("%.2f", bottomPhysicalDepth));
      }

      if (showPingNumber.getBooleanValue()) {
         addRow(toolTip, "Ping number", String.valueOf(pingIndex.getPingNumber()));
      }

      if (showVesselDistance.getBooleanValue()) {
         double vesselDistance = lsss.getInterpretationSettings().getDataFileSet().getVesselDistanceUncorrectedForWrapAround(pingIndex);
         addRow(toolTip, "Distance [nmi]", Utils.format("%.3f", vesselDistance));
         addRow(toolTip, "Distance [km]", Utils.format("%.3f", KoronaUtils.nmiToMeter(vesselDistance) / 1000));
      }

      if (showVesselSpeed.getBooleanValue()) {
         double knots = DataUtils.getKnots(ping, lsss.getInterpretationSettings().getDataFileSet());
         addRow(toolTip, "Speed [knots]", Utils.format("%.3f", knots));
      }

      if (showSampleNumber.getBooleanValue()) {
         addRow(toolTip, "Sample number", sampleIndex >= 0 ? String.valueOf(sampleIndex) : null);
      }

      if (showSvValue.getBooleanValue()) {
         String text = null;
         if (sampleIndex >= 0) {
            text = Utils.format("%.3f", powerData.getLogSv()[sampleIndex]);
         }
         addRow(toolTip, "Sv [dB]", text);
      }

      if (showTSCValue.getBooleanValue()) {
         String text = null;
         if (sampleIndex >= 0 && powerData.getAngleData() != null) {
            text = Utils.format("%.3f", powerData.getTSC(sampleIndex));
         }
         addRow(toolTip, "TSC [dB]", text);
      }

      if (showTSUValue.getBooleanValue()) {
         String text = null;
         if (sampleIndex >= 0) {
            text = Utils.format("%.3f", powerData.getTSU(sampleIndex));
         }
         addRow(toolTip, "TSU [dB]", text);
      }

      if (showPulseCompression.getBooleanValue()) {
         String textRe = null;
         String textIm = null;
         if (sampleIndex >= 0 && channelData instanceof BroadbandData broadbandData) {
            ComplexArray averagePulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();
            textRe = Double.toString(MathUtils.roundToNumberOfDigits(averagePulseCompressedSignal.re(sampleIndex), 6));
            textIm = Double.toString(MathUtils.roundToNumberOfDigits(averagePulseCompressedSignal.im(sampleIndex), 6));
         }
         addRow(toolTip, "Average real [W]", textRe);
         addRow(toolTip, "Average imag [V a]", textIm);
      }

      if (showAlongAngle.getBooleanValue()) {
         String text = null;
         if (sampleIndex >= 0 && powerData.getAngleData() != null) {
            text = Utils.format("%.3f", powerData.getMechanicalAlongAngle(sampleIndex));
         }
         addRow(toolTip, "Along angle [deg]", text);
      }

      if (showAthwartAngle.getBooleanValue()) {
         String text = null;
         if (sampleIndex >= 0 && powerData.getAngleData() != null) {
            text = Utils.format("%.3f", powerData.getMechanicalAthwartAngle(sampleIndex));
         }
         addRow(toolTip, "Athwart angle [deg]", text);
      }

      if (showSvThresholds.getBooleanValue()) {
         FloatRange logSvRange = lsss.getRegionManager().getThresholdManager().getLogSvRange(pingIndex);
         addRow(toolTip, "Sv thresholds [dB]", logSvRange.toString());
      }

      if (showRegionLabels.getBooleanValue()) {
         String text = null;
         if (region != null) {
            text = region.getLabels().stream()
                  .sorted()
                  .collect(Collectors.joining(", "));
         }
         addRow(toolTip, "Region labels", text);
      }

      if (showObjectNumber.getBooleanValue()) {
         int objectNumber = -1;
         if (school != null) {
            objectNumber = school.getObjectNumber();
         } else {
            Scatter scatter = lsss.getInterpretationSummary().getScatterSet().getScatter(echogramModule.getScatterTypeEnum(), lsss.getInterpretationSettings().getFrequency(), pingIndex, z);
            if (scatter != null) {
               objectNumber = scatter.getCompId().getObject();
            }
         }
         addRow(toolTip, "Object number", objectNumber >= 0 ? String.valueOf(objectNumber) : null);
      }

      if (showCategory.getBooleanValue()) {
         String text = null;
         Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
         Cac0Datagram cac0Datagram = ping.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
         if (cad0Datagram != null && cac0Datagram != null) {
            StringBuilder textBuilder = new StringBuilder();
            int index = cad0Datagram.depthToIndex(depth);
            for (int i = 0; i < cad0Datagram.getCategoryCount(); i++) {
               byte categoryNumber = cad0Datagram.getCategory(i, index);
               Cac0Datagram.Category category = cac0Datagram.numberToCategory(categoryNumber);
               if (category.isUnknown()) {
                  if (i == 0) {
                     textBuilder.append(category.getName());
                  }
                  break;
               }
               if (i > 0) {
                  textBuilder.append(", ");
               }
               float probability = cad0Datagram.getProbability(i, index);
               String percentString = probability == 1 ? "100" : Utils.format("%.1f", 100 * probability);
               textBuilder.append(category.getName() + " (" + percentString + "%)");
            }
            text = textBuilder.toString();
         }
         addRow(toolTip, "Category", text);
      }

      if (showPlankton.getBooleanValue()) {
         Pid0Datagram pid0Datagram = ping.getPingItem(Pid0Datagram.class);
         Pic0Datagram.PlanktonCategory planktonCategory = null;
         Pid0Datagram.PlanktonData planktonSample = null;
         if (pid0Datagram != null) {
            Pic0Datagram pic0Datagram = lsss.getInterpretationSettings().getDataFileSet().getConfigurationItem(Pic0Datagram.class);
            if (pic0Datagram != null) {
               int index = pid0Datagram.depthToIndex(depth);
               if (index >= 0 && index < pid0Datagram.getCount()) {
                  List<Pid0Datagram.PlanktonSample> planktonSamples = pid0Datagram.getPlanktonSamples(pic0Datagram);
                  for (Pid0Datagram.PlanktonData planktonData : planktonSamples.get(index).getPlanktonData()) {
                     planktonCategory = planktonData.getPlanktonCategory();
                     planktonSample = planktonData;
                  }
               }
            }
         }
         String text = null;
         if (planktonCategory != null) {
            text = planktonCategory.getLegend() + " (" + Utils.format("%.3f", planktonSample.getResidual()) + ")";
         }
         addRow(toolTip, "Plankton", text);
      }

      if (showScatter.getBooleanValue()) {
         InterpretationSummary.ScatterSet scatterSet = lsss.getInterpretationSummary().getScatterSet();
         Scatter schoolScatter = scatterSet.getScatter(echogramModule.getSchoolScatterTypeEnum(), lsss.getInterpretationSettings().getFrequency(), pingIndex, z);
         Scatter backgroundScatter = scatterSet.getScatter(echogramModule.getScatterTypeEnum(), lsss.getInterpretationSettings().getFrequency(), pingIndex, z);
         Scatter scatter = schoolScatter != null ? schoolScatter : backgroundScatter;
         if (scatter != null) {
            addRowCol2(toolTip, "Scatter:");
            for (Map.Entry<String, Object> entry : DatabaseUtils.getValues(lsss.getDatabaseManager().getDatabaseConnection(), scatter).entrySet()) {
               addRowItem(toolTip, entry.getKey(), entry.getValue().toString());
            }
         } else {
            addRow(toolTip, "Scatter:", null);
         }
      }

      if (showSchoolParameters.getBooleanValue()) {
         if (school != null && lsss.getConfigurationManager().getSurveyMiscConf().computeSchoolParameters.getBooleanValue()) {
            SchoolParameterModule schoolParameterModule = lsss.getModuleManager().getModule(SchoolParameterModule.class);
            addRowCol2(toolTip, "School parameters");
            SchoolParameters schoolParameters = school.getParameters();
            if (schoolParameters.isUpToDate()) {
               addRowItem(toolTip, "Data processed", Boolean.toString(schoolParameters.dataProcessed()));
               addSchoolParameters(toolTip, schoolParameterModule, schoolParameters.values());
               Map<String, Float> perChannelValues = schoolParameters.perChannelValues().get(lsss.getInterpretationSettings().getChannel());
               if (perChannelValues != null) {
                  addSchoolParameters(toolTip, schoolParameterModule, perChannelValues);
               }
            }
         } else {
            addRow(toolTip, "School parameters", null);
         }
      }

      if (showStoringSettings.getBooleanValue()) {
         StoringIntervalConfig intervalConfig = lsss.getRegionManager().getStoringConfigManager().get(pingIndex);
         if (intervalConfig != null) {
            addRowCol2(toolTip, "Storing settings");
            addRowItem(toolTip, "Frequencies [kHz]", Joiner.on(", ").join(intervalConfig.kHz()));
            String dataDir;
            if (intervalConfig.dataDir() == 0) {
               dataDir = DataConfLSSS.RAW_SUB_DIR.parameterName().displayName();
            } else if (intervalConfig.dataDir() == 1) {
               dataDir = DataConfLSSS.KORONA_SUB_DIR.parameterName().displayName();
            } else {
               dataDir = String.valueOf(intervalConfig.dataDir());
            }
            addRowItem(toolTip, "Data dir", dataDir);
            addRowItem(toolTip, "Pelagic mode", intervalConfig.pelagicMode() ? "Yes" : "No");
            addRowItem(toolTip, "Quality", QualityEnum.valueToText(intervalConfig.quality()));
         } else {
            addRow(toolTip, "Storing settings", null);
         }
      }

      if (showNmea.getBooleanValue()) {
         List<NmeaPingItem> pingItems = ping.getPingItems(NmeaPingItem.class).toList();
         addRowCol2(toolTip, pingItems.size() + " NMEA datagrams:");
         pingItems.forEach(nmeaPingItem -> {
            addRowCol2(toolTip, " - " + nmeaPingItem.getNmeaString());
         });
      }

      toolTip.append("</table>");
      return toolTip.toString();
   }

   private static String htmlEscape(String text) {
      return HtmlEscapers.htmlEscaper().escape(text).replace(" ", "&nbsp;");
   }

   private static void addSchoolParameters(StringBuilder tooltip, SchoolParameterModule schoolParameterModule, Map<String, Float> values) {
      values.forEach((key, value) -> {
         SchoolParameter schoolParameter = schoolParameterModule.getSchoolParameters().get(key);
         if (schoolParameter == null) {
            return;
         }
         String nameAndUnit = Utils.nameAndUnit(schoolParameter.name().displayName(), schoolParameter.unit());
         addRowItem(tooltip, nameAndUnit, Utils.format("%.2f", value));
      });
   }

   private static void addRow(StringBuilder tooltip, String name, @Nullable String value) {
      if (value == null) {
         tooltip.append("<tr><td style='white-space: nowrap; color: gray; font-style: italic;'>").append(htmlEscape(name)).append("</td></tr>");
      } else {
         tooltip.append("<tr><td style='white-space: nowrap;'>").append(htmlEscape(name))
               .append("</td><td align=right>").append(htmlEscape(value)).append("</td></tr>");
      }
   }

   private static void addRowItem(StringBuilder tooltip, String name, @Nullable String value) {
      addRow(tooltip, " - " + name, value);
   }

   private static void addRowCol2(StringBuilder tooltip, String text) {
      tooltip.append("<tr><td colspan=2 style='white-space: nowrap;'>").append(htmlEscape(text)).append("</td></tr>");
   }
}
