package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.DataLoadingMode;
import no.imr.lsss.server.pojo.DataConfig;
import no.imr.lsss.server.pojo.ping.PojoChannel;
import no.imr.lsss.server.pojo.ping.PojoComplexArray;
import no.imr.lsss.server.pojo.ping.PojoNmea;
import no.imr.lsss.server.pojo.ping.PojoPing;
import no.imr.lsss.server.pojo.values.BooleanValue;
import no.imr.lsss.server.pojo.values.FloatValue;
import no.imr.lsss.server.pojo.values.StringValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.lsss.server.util.ValueAndPingMapping;
import no.imr.tools.annotations.ReflectionEntryPoint;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.swing.SwingDelayer;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class DataResource {
   private final LSSS lsss;

   DataResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Path("config")
   @Produces(MediaType.APPLICATION_JSON)
   public DataConfig getConfig() {
      return new DataConfig(lsss.getInterpretationSettings().getDataFileSet());
   }

   @GET
   @Path("frequencies")
   @Produces(MediaType.APPLICATION_JSON)
   public double[] getFrequencies() {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      RawFileConfiguration rawFileConfiguration = dataFileSet.getRawFileConfiguration();
      return rawFileConfiguration.getTransducers().stream()
            .mapToDouble(RawFileTransducer::getFrequency)
            .toArray();
   }

   @GET
   @Path("frequency")
   @Produces(MediaType.APPLICATION_JSON)
   public FloatValue getFrequency() {
      DataFileSet dataFileSet = getNonEmptyDataFileSet();
      int channel = lsss.getInterpretationSettings().getChannel();
      float frequency = dataFileSet.getFrequency(channel);
      return new FloatValue(frequency);
   }

   @POST
   @Path("frequency")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setFrequency(FloatValue frequency) {
      DataFileSet dataFileSet = getNonEmptyDataFileSet();
      int channel = dataFileSet.firstChannelClosestTo(frequency.value);
      if (channel < 0) {
         throw new BadRequestException("Frequency not available: " + frequency.value);
      }
      lsss.getInterpretationSettings().setChannel(channel);
   }

   private DataFileSet getNonEmptyDataFileSet() {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      if (dataFileSet.isEmpty()) {
         throw new BadRequestException("No data");
      }
      return dataFileSet;
   }

   @GET
   @Path("mode")
   @Produces(MediaType.APPLICATION_JSON)
   public StringValue getMode() {
      return new StringValue(lsss.getInterpretationSettings().getDataLoadingMode().name());
   }

   @POST
   @Path("mode")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setMode(StringValue stringValue) {
      DataLoadingMode mode = LsssServerUtils.nameToEnum(DataLoadingMode.class, stringValue.value);
      lsss.getInterpretationSettings().setDataLoadingMode(mode);
   }

   @GET
   @Path("ping")
   @Produces(MediaType.APPLICATION_JSON)
   public PojoPing getPing(@BeanParam PingRequest pingRequest) {
      DataFileSet dataFileSet = getNonEmptyDataFileSet();
      PingIndex pingIndex = toPingIndex(pingRequest, dataFileSet);
      return toPojoPing(pingRequest, dataFileSet, pingIndex);
   }

   @GET
   @Path("pings")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<PojoPing> getPings(@BeanParam PingsRequest pingsRequest) {
      DataFileSet dataFileSet = getNonEmptyDataFileSet();
      PingIndex beginPingIndex = toPingIndex(pingsRequest, dataFileSet);
      if (pingsRequest.pingCount < 0) {
         throw new BadRequestException("Negative pingCount");
      }
      PingIndex endPingIndex = dataFileSet.getClosestPingIndex(beginPingIndex.getPingNumber() + pingsRequest.pingCount, PingMapping.NUMBER);
      return dataFileSet.getPingIndexStream(PingRange.of(beginPingIndex, endPingIndex))
            .map(pingIndex -> toPojoPing(pingsRequest, dataFileSet, pingIndex));
   }

   private static PingIndex toPingIndex(PingRequest pingRequest, DataFileSet dataFileSet) {
      ValueAndPingMapping valueAndPingMapping = ValueAndPingMapping.from(pingRequest.time, pingRequest.pingNumber, pingRequest.vesselDistance);
      if (valueAndPingMapping == null) {
         throw new BadRequestException("Missing query parameter");
      }
      PingIndex pingIndex = LsssServerUtils.toPingIndexOrNull(dataFileSet, valueAndPingMapping);
      if (pingIndex == null) {
         throw new NotFoundException("Ping not found");
      }
      return pingIndex;
   }

   private static PojoPing toPojoPing(PingRequest pingRequest, DataFileSet dataFileSet, PingIndex pingIndex) {
      if (pingRequest.minDepth != null && pingRequest.maxDepth != null && pingRequest.minDepth > pingRequest.maxDepth) {
         throw new BadRequestException("minDepth > maxDepth");
      }
      Ping ping = dataFileSet.getPing(pingIndex);
      PojoPing pojoPing = new PojoPing(ping);
      pojoPing.channels = ping.getNonNullChannelDatas()
            .map(channelData -> {
               PowerData powerData = channelData.getPowerData();
               int beginIndex = pingRequest.minDepth != null ? powerData.depthToClampedSampleIndex(pingRequest.minDepth) : 0;
               int endIndex = pingRequest.maxDepth != null ? powerData.depthToClampedSampleIndex(pingRequest.maxDepth) : powerData.getCount();
               int n = endIndex - beginIndex;
               PojoChannel pojoChannel = new PojoChannel(powerData);
               pojoChannel.offset += beginIndex;
               if (pingRequest.all || pingRequest.sv) {
                  pojoChannel.sv = Arrays.copyOfRange(powerData.getLogSv(), beginIndex, endIndex);
                  ArrayMath.round(pojoChannel.sv, 100);
               }
               if (pingRequest.all || pingRequest.tsc) {
                  pojoChannel.tsc = new float[n];
                  for (int i = 0; i < n; i++) {
                     pojoChannel.tsc[i] = powerData.getTSC(beginIndex + i);
                  }
                  ArrayMath.round(pojoChannel.tsc, 100);
               }
               if (pingRequest.all || pingRequest.tsu) {
                  pojoChannel.tsu = new float[n];
                  for (int i = 0; i < n; i++) {
                     pojoChannel.tsu[i] = powerData.getTSU(beginIndex + i);
                  }
                  ArrayMath.round(pojoChannel.tsu, 100);
               }
               if (pingRequest.all || pingRequest.angles) {
                  pojoChannel.alongshipAngle = new float[n];
                  pojoChannel.athwartshipAngle = new float[n];
                  for (int i = 0; i < n; i++) {
                     pojoChannel.alongshipAngle[i] = powerData.getMechanicalAlongAngle(beginIndex + i);
                     pojoChannel.athwartshipAngle[i] = powerData.getMechanicalAthwartAngle(beginIndex + i);
                  }
                  ArrayMath.round(pojoChannel.alongshipAngle, 100);
                  ArrayMath.round(pojoChannel.athwartshipAngle, 100);
               }
               if (pingRequest.pulseCompressed && channelData instanceof BroadbandData broadbandData) {
                  pojoChannel.pulseCompressed = new PojoComplexArray(broadbandData.getAveragePulseCompressedSignal(), beginIndex, endIndex);
               }
               if (pingRequest.complex && channelData instanceof ComplexChannelData complexChannelData) {
                  pojoChannel.complex = IntStream.range(0, complexChannelData.getSectorCount())
                        .mapToObj(sector -> new PojoComplexArray(sector, complexChannelData, beginIndex, endIndex))
                        .toList();
               }
               return pojoChannel;
            })
            .toList();
      if (pingRequest.all || pingRequest.nmea) {
         pojoPing.nmea = ping.getPingItems(NmeaPingItem.class)
               .map(PojoNmea::new)
               .toList();
      }
      return pojoPing;
   }

   @GET
   @Path("wait")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue waitUntilFinished() throws InterruptedException {
      lsss.getInterpretationSettings().waitUntilFinished();

      CountDownLatch countDownLatch = new CountDownLatch(1);
      SwingDelayer.invokeLater(new Object(), countDownLatch::countDown);
      countDownLatch.await();

      return new BooleanValue(true);
   }

   public static class PingRequest {
      @QueryParam("time")
      public @Nullable String time;

      @QueryParam("pingNumber")
      public @Nullable Long pingNumber;

      @QueryParam("vesselDistance")
      public @Nullable Double vesselDistance;

      @QueryParam("all")
      public boolean all;

      @QueryParam("angles")
      public boolean angles;

      @QueryParam("sv")
      public boolean sv;

      @QueryParam("tsc")
      public boolean tsc;

      @QueryParam("tsu")
      public boolean tsu;

      @QueryParam("pulseCompressed")
      public boolean pulseCompressed;

      @QueryParam("complex")
      public boolean complex;

      @QueryParam("nmea")
      public boolean nmea;

      @QueryParam("minDepth")
      public @Nullable Float minDepth;

      @QueryParam("maxDepth")
      public @Nullable Float maxDepth;

      @ReflectionEntryPoint
      public PingRequest() {
      }
   }

   public static final class PingsRequest extends PingRequest {
      @QueryParam("pingCount")
      @DefaultValue("1")
      public long pingCount;

      @ReflectionEntryPoint
      public PingsRequest() {
      }
   }
}
