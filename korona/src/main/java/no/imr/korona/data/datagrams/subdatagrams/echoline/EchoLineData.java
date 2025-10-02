package no.imr.korona.data.datagrams.subdatagrams.echoline;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.LsssDatagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class EchoLineData extends ChannelData {

   private List<EchoLine> echoLines;
   private final float effectivePulseDuration;
   private @Nullable PowerData powerData;

   EchoLineData(EchoLineSubDatagram echoLineSubDatagram, PingConfiguration pingConfiguration, float effectivePulseDuration) {
      super(echoLineSubDatagram.getRaw0Datagram());

      this.effectivePulseDuration = effectivePulseDuration;
      setPingConfiguration(pingConfiguration);
      PowerData powerData = new PowerData(this, effectivePulseDuration);
      echoLines = echoLineSubDatagram.getPowerEchoLines().stream()
            .map(powerEchoLine -> {
               float[] sv = new float[powerEchoLine.shortPower().length];
               TvgArray tvg = powerData.getTVGArray();
               for (int i = 0; i < sv.length; i++) {
                  sv[i] = powerData.shortPowerToNpi(powerEchoLine.shortPower()[i]) * tvg.get(i + powerEchoLine.startSample());
               }
               byte[] angles = powerEchoLine.angles();
               float[] electricalAngles = angles != null
                     ? AngleData.bytesToElectricalAngles(angles, powerData.getTransducer())
                     : null;
               return new EchoLine(powerEchoLine.startSample(), sv, electricalAngles);
            })
            .toList();
   }

   private EchoLineData(ChannelData channelData, List<EchoLine> echoLines, float effectivePulseDuration) {
      super(channelData);

      this.echoLines = echoLines;
      this.effectivePulseDuration = effectivePulseDuration;
   }

   public EchoLineData(PowerData powerData, List<EchoLine> echoLines) {
      this(powerData, echoLines, powerData.getEffectivePulseDuration());
   }

   @Override
   public String getDataTypeName() {
      return "Echo line data";
   }

   public List<EchoLine> getEchoLines() {
      return echoLines;
   }

   public void setEchoLines(List<EchoLine> echoLines) {
      this.echoLines = echoLines;
   }

   @Override
   public PowerData getPowerData() {
      PowerData powerData = this.powerData;
      if (powerData == null) {
         powerData = new PowerData(this);
         powerData.setSv(computeSv());
         if (hasAngles()) {
            powerData.setElectricAngles(computeElectricalAngles());
         }
         powerData.setReadOnly(this);
         this.powerData = powerData;
      }
      return powerData;
   }

   private float[] computeSv() {
      float[] sv = new float[getCount()];
      for (EchoLine echoLine : echoLines) {
         System.arraycopy(echoLine.sv, 0, sv, echoLine.startSample, echoLine.sv.length);
      }
      return sv;
   }

   private float[] computeElectricalAngles() {
      float[] electricalAngles = new float[2 * getCount()];
      for (EchoLine echoLine : echoLines) {
         if (echoLine.electricalAngles != null) {
            System.arraycopy(echoLine.electricalAngles, 0,
                  electricalAngles, 2 * echoLine.startSample, echoLine.electricalAngles.length);
         }
      }
      return electricalAngles;
   }

   private boolean hasAngles() {
      return !echoLines.isEmpty() && echoLines.getFirst().electricalAngles != null;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      PowerData powerData = new PowerData(this, effectivePulseDuration);
      TvgArray tvg = getTVGArray();
      List<EchoLineSubDatagram.PowerEchoLine> powerEchoLines = echoLines.stream()
            .map(echoLine -> {
               short[] shortPower = new short[echoLine.sv.length];
               for (int i = 0; i < shortPower.length; i++) {
                  shortPower[i] = powerData.npiToShortPower(echoLine.sv[i] / tvg.get(i + echoLine.startSample));
               }
               byte[] angles = echoLine.electricalAngles != null
                     ? AngleData.electricalAnglesToBytes(echoLine.electricalAngles, getTransducer())
                     : null;
               return new EchoLineSubDatagram.PowerEchoLine(echoLine.startSample, shortPower, angles);
            })
            .toList();
      EchoLineSubDatagram echoLineSubDatagram = new EchoLineSubDatagram(getNTDate(), powerEchoLines);
      setRaw0DatagramParameters(echoLineSubDatagram.getRaw0Datagram());
      echoLineSubDatagram.getRaw0Datagram().mode = Raw0Datagram.DATA_TYPE_POWER;
      if (hasAngles()) {
         echoLineSubDatagram.getRaw0Datagram().mode |= Raw0Datagram.DATA_TYPE_ANGLES;
      }
      LsssDatagram lsssDatagram = new LsssDatagram(echoLineSubDatagram);
      if (effectivePulseDuration != PowerData.MISSING_EFFECTIVE_PULSE_DURATION) {
         return List.of(powerData.toXml0ParameterDatagram(), lsssDatagram);
      } else {
         return List.of(lsssDatagram);
      }
   }

   @Override
   public EchoLineData makeCopy() {
      List<EchoLine> echoLinesCopy = echoLines.stream()
            .map(EchoLine::makeCopy)
            .toList();
      return new EchoLineData(this, echoLinesCopy, effectivePulseDuration);
   }

   @Override
   public void removeAngles() {
      throwExceptionIfReadOnly();
      echoLines = echoLines.stream()
            .map(echoLine -> new EchoLine(echoLine.startSample, echoLine.sv, null))
            .toList();
      powerData = null;
   }

   @Override
   public void reduceData(int beginSampleIndex, int endSampleIndex) {
      throwExceptionIfReadOnly();
      echoLines = echoLines.stream()
            .map(echoLine -> {
               int originalBegin = echoLine.startSample();
               int originalEnd = originalBegin + echoLine.count();
               int begin = Math.max(beginSampleIndex, originalBegin);
               int end = Math.min(endSampleIndex, originalEnd);
               if (begin >= end) {
                  return null;
               }
               if (begin == originalBegin && end == originalEnd) {
                  return echoLine;
               }
               return new EchoLine(
                     begin,
                     Arrays.copyOfRange(echoLine.sv, begin - originalBegin, end - originalBegin),
                     echoLine.electricalAngles != null
                           ? Arrays.copyOfRange(echoLine.electricalAngles, 2 * (begin - originalBegin), 2 * (end - originalBegin))
                           : null
               );
            })
            .filter(Objects::nonNull)
            .toList();

      powerData = null;
   }

   public record EchoLine(
         int startSample,
         float[] sv,
         float @Nullable [] electricalAngles
   ) {
      public EchoLine {
         if (electricalAngles != null && electricalAngles.length != 2 * sv.length) {
            throw new IllegalArgumentException("Electrical angles has length " + electricalAngles.length + " != " + 2 * sv.length);
         }
      }

      public int count() {
         return sv.length;
      }

      private EchoLine makeCopy() {
         return new EchoLine(startSample, sv.clone(), electricalAngles != null ? electricalAngles.clone() : null);
      }
   }
}
