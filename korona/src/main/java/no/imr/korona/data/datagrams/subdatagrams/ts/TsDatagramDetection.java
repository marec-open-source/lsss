package no.imr.korona.data.datagrams.subdatagrams.ts;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ts.TSDetection;
import no.imr.tools.range.FloatRange;

import java.nio.ByteBuffer;

public final class TsDatagramDetection {
   static final int BYTE_SIZE = 8 * 4;

   public final float sv;
   public final float tsc;
   public final float tsu;
   public final float alongshipAngle;
   public final float athwartshipAngle;
   public final FloatRange depthRange;
   public final float peakDepth;

   public TsDatagramDetection(ChannelData channelData, TSDetection tsDetection) {
      PowerData powerData = channelData.getPowerData();
      sv = powerData.getSv()[tsDetection.peakIndex()];
      tsc = powerData.getTSC(tsDetection.peakIndex());
      tsu = powerData.getTSU(tsDetection.peakIndex());
      alongshipAngle = powerData.getMechanicalAlongAngle(tsDetection.peakIndex());
      athwartshipAngle = powerData.getMechanicalAthwartAngle(tsDetection.peakIndex());
      depthRange = FloatRange.of(
            channelData.getSampleDepth(tsDetection.beginIndex()),
            channelData.getSampleDepth(tsDetection.endIndex()));
      peakDepth = channelData.getSampleDepth(tsDetection.peakIndex());
   }

   TsDatagramDetection(ByteBuffer byteBuffer) throws DatagramFormatException {
      sv = byteBuffer.getFloat();
      tsc = byteBuffer.getFloat();
      tsu = byteBuffer.getFloat();
      alongshipAngle = byteBuffer.getFloat();
      athwartshipAngle = byteBuffer.getFloat();
      depthRange = ByteBufferUtils.readFloatRange(byteBuffer);
      peakDepth = byteBuffer.getFloat();
   }

   void write(ByteBuffer byteBuffer) {
      byteBuffer.putFloat(sv);
      byteBuffer.putFloat(tsc);
      byteBuffer.putFloat(tsu);
      byteBuffer.putFloat(alongshipAngle);
      byteBuffer.putFloat(athwartshipAngle);
      ByteBufferUtils.writeFloatRange(byteBuffer, depthRange);
      byteBuffer.putFloat(peakDepth);
   }
}
