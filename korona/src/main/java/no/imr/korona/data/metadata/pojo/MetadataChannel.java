package no.imr.korona.data.metadata.pojo;

import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.util.Arrays;

public final class MetadataChannel {
   public String channelId;
   public int beamType;
   public float frequency;
   public float gain;
   public float equivalentBeamAngle;
   public float beamWidthAlongship;
   public float beamWidthAthwartship;
   public float angleSensitivityAlongship;
   public float angleSensitivityAthwartship;
   public float angleOffsetAlongship;
   public float angleOffsetAthwartship;
   public float[] pos;
   public float[] dir;
   public float[] pulseDurationTable;
   public float[] gainTable;
   public float[] saCorrectionTable;

   public MetadataChannel(RawFileTransducer rawFileTransducer) {
      channelId = rawFileTransducer.getChannelId();
      beamType = rawFileTransducer.getBeamType();
      frequency = rawFileTransducer.getFrequency();
      gain = rawFileTransducer.getGain();
      equivalentBeamAngle = rawFileTransducer.getEquivalentBeamAngle();
      beamWidthAlongship = rawFileTransducer.getBeamWidthAlongship();
      beamWidthAthwartship = rawFileTransducer.getBeamWidthAthwartship();
      angleSensitivityAlongship = rawFileTransducer.getAngleSensitivityAlongship();
      angleSensitivityAthwartship = rawFileTransducer.getAngleSensitivityAthwartship();
      angleOffsetAlongship = rawFileTransducer.getAngleOffsetAlongship();
      angleOffsetAthwartship = rawFileTransducer.getAngleOffsetAthwartship();
      pos = new float[]{rawFileTransducer.getPos().x(), rawFileTransducer.getPos().y(), rawFileTransducer.getPos().z()};
      dir = new float[]{rawFileTransducer.getDir().x(), rawFileTransducer.getDir().y(), rawFileTransducer.getDir().z()};
      pulseDurationTable = rawFileTransducer.getPulseDurationTable();
      gainTable = rawFileTransducer.getGainTable();
      saCorrectionTable = rawFileTransducer.getSaCorrectionTable();
   }

   @Override
   public String toString() {
      return "MetadataChannel{" +
            "channelId='" + channelId + '\'' +
            ", beamType=" + beamType +
            ", frequency=" + frequency +
            ", gain=" + gain +
            ", equivalentBeamAngle=" + equivalentBeamAngle +
            ", beamWidthAlongship=" + beamWidthAlongship +
            ", beamWidthAthwartship=" + beamWidthAthwartship +
            ", angleSensitivityAlongship=" + angleSensitivityAlongship +
            ", angleSensitivityAthwartship=" + angleSensitivityAthwartship +
            ", angleOffsetAlongship=" + angleOffsetAlongship +
            ", angleOffsetAthwartship=" + angleOffsetAthwartship +
            ", pos=" + Arrays.toString(pos) +
            ", dir=" + Arrays.toString(dir) +
            ", pulseDurationTable=" + Arrays.toString(pulseDurationTable) +
            ", gainTable=" + Arrays.toString(gainTable) +
            ", saCorrectionTable=" + Arrays.toString(saCorrectionTable) +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof MetadataChannel that
            && channelId.equals(that.channelId)
            && beamType == that.beamType
            && Float.floatToIntBits(frequency) == Float.floatToIntBits(that.frequency)
            && Float.floatToIntBits(gain) == Float.floatToIntBits(that.gain)
            && Float.floatToIntBits(equivalentBeamAngle) == Float.floatToIntBits(that.equivalentBeamAngle)
            && Float.floatToIntBits(beamWidthAlongship) == Float.floatToIntBits(that.beamWidthAlongship)
            && Float.floatToIntBits(beamWidthAthwartship) == Float.floatToIntBits(that.beamWidthAthwartship)
            && Float.floatToIntBits(angleSensitivityAlongship) == Float.floatToIntBits(that.angleSensitivityAlongship)
            && Float.floatToIntBits(angleSensitivityAthwartship) == Float.floatToIntBits(that.angleSensitivityAthwartship)
            && Float.floatToIntBits(angleOffsetAlongship) == Float.floatToIntBits(that.angleOffsetAlongship)
            && Float.floatToIntBits(angleOffsetAthwartship) == Float.floatToIntBits(that.angleOffsetAthwartship)
            && Arrays.equals(pos, that.pos)
            && Arrays.equals(dir, that.dir)
            && Arrays.equals(pulseDurationTable, that.pulseDurationTable)
            && Arrays.equals(gainTable, that.gainTable)
            && Arrays.equals(saCorrectionTable, that.saCorrectionTable);
   }

   @Override
   public int hashCode() {
      int result = channelId.hashCode();
      result = 31 * result + beamType;
      result = 31 * result + Float.hashCode(frequency);
      result = 31 * result + Float.hashCode(gain);
      result = 31 * result + Float.hashCode(equivalentBeamAngle);
      result = 31 * result + Float.hashCode(beamWidthAlongship);
      result = 31 * result + Float.hashCode(beamWidthAthwartship);
      result = 31 * result + Float.hashCode(angleSensitivityAlongship);
      result = 31 * result + Float.hashCode(angleSensitivityAthwartship);
      result = 31 * result + Float.hashCode(angleOffsetAlongship);
      result = 31 * result + Float.hashCode(angleOffsetAthwartship);
      result = 31 * result + Arrays.hashCode(pos);
      result = 31 * result + Arrays.hashCode(dir);
      result = 31 * result + Arrays.hashCode(pulseDurationTable);
      result = 31 * result + Arrays.hashCode(gainTable);
      result = 31 * result + Arrays.hashCode(saCorrectionTable);
      return result;
   }
}
