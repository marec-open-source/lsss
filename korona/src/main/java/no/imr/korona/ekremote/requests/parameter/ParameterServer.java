package no.imr.korona.ekremote.requests.parameter;

import java.util.List;

/**
 * Parameter names.
 * <p>
 * See also <a href="http://www.gebruikershandleiding.com/Simrad-ER60/preview-handleiding-16819.html?page=0180">http://www.gebruikershandleiding.com/Simrad-ER60/preview-handleiding-16819.html?page=0180</a>
 */
public final class ParameterServer {
   public static final String TYPE_3 = "3"; // int ?
   public static final String TYPE_5 = "5"; // float ?
   public static final String TYPE_8 = "8"; // String ?
   public static final String TYPE_8200 = "8200"; // List of String?
   public static final String TYPE_UNKNOWN = ""; // ?

   private ParameterServer() {
   }

   public static List<? extends Class<? extends NormalParameter>> getAllParameterEnums() {
      return List.of(
            AcousticDeviceSynchroniser.class,
            OperationControl.class,
            OwnShip.class,
            OwnShip.EnvironmentData.class,
            RemoteCommandDispatcher.class,
            SounderStorageManager.class,
            TransceiverMgr.class);
   }

   public static List<? extends Class<? extends PerChannelParameter>> getAllPerChannelParameterEnums() {
      return List.of(
            ProcessingMgr.ChannelProcessingCommon.PerChannel.class,
            TransceiverMgr.PerChannel.class);
   }

   public interface Parameter {
      String getType();
   }

   public interface NormalParameter extends Parameter {
      String getName();
   }

   public interface PerChannelParameter extends Parameter {
      String getName(String channelId);
   }

   public enum RemoteCommandDispatcher implements NormalParameter {
      ClientTimeoutLimit(TYPE_3);

      private final String type;

      RemoteCommandDispatcher(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "RemoteCommandDispatcher/" + name();
      }

      @Override
      public String getType() {
         return type;
      }
   }

   public enum OperationControl implements NormalParameter {
      OperationMode(TYPE_3);

      private final String type;

      OperationControl(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "OperationControl/" + name();
      }

      @Override
      public String getType() {
         return type;
      }
   }

   public enum AcousticDeviceSynchroniser implements NormalParameter {
      SyncMode(TYPE_3),
      Interval(TYPE_3);

      private final String type;

      AcousticDeviceSynchroniser(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "AcousticDeviceSynchroniser/" + name();
      }

      @Override
      public String getType() {
         return type;
      }
   }

   public enum SounderStorageManager implements NormalParameter {
      SaveRawData(TYPE_3),
      SaveIndexFile(TYPE_3),
      SaveBottomDepthFile(TYPE_3),
      SampleRange(TYPE_3);

      private final String type;

      SounderStorageManager(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "SounderStorageManager/" + name();
      }

      @Override
      public String getType() {
         return type;
      }
   }

   public enum OwnShip implements NormalParameter {
      Speed(TYPE_5),
      Latitude(TYPE_5),
      Longitude(TYPE_5),
      Heave(TYPE_5),
      Roll(TYPE_5),
      Pitch(TYPE_5),
      VesselDistance(TYPE_5);

      private final String type;

      OwnShip(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "OwnShip/" + name();
      }

      @Override
      public String getType() {
         return type;
      }

      public enum EnvironmentData implements NormalParameter {
         Temperature(TYPE_5),
         Salinity(TYPE_5),
         SoundVelocity(TYPE_5);

         private final String type;

         EnvironmentData(String type) {
            this.type = type;
         }

         @Override
         public String getName() {
            return "OwnShip/EnvironmentData/" + name();
         }

         @Override
         public String getType() {
            return type;
         }
      }
   }

   public enum TransceiverMgr implements NormalParameter {
      Channels(TYPE_8200),
      PingTime(TYPE_UNKNOWN),
      Latitude(TYPE_5),
      Longitude(TYPE_5),
      Heave(TYPE_5),
      Roll(TYPE_5),
      Pitch(TYPE_5),
      VesselDistance(TYPE_5);

      private final String type;

      TransceiverMgr(String type) {
         this.type = type;
      }

      @Override
      public String getName() {
         return "TransceiverMgr/" + name();
      }

      @Override
      public String getType() {
         return type;
      }

      public enum PerChannel implements PerChannelParameter {
         Frequency(TYPE_5),
         PulseLength(TYPE_5),
         SampleInterval(TYPE_5),
         TransmitPower(TYPE_5),
         AbsorptionCoefficient(TYPE_5),
         SoundVelocity(TYPE_5),
         TransducerName(TYPE_8),
         TransducerDepth(TYPE_5),
         EquivalentBeamAngle(TYPE_5),
         AngleSensitivityAlongship(TYPE_5),
         AngleSensitivityAthwartship(TYPE_5),
         BeamWidthAlongship(TYPE_5),
         BeamWidthAthwartship(TYPE_5),
         AngleOffsetAlongship(TYPE_5),
         AngleOffsetAthwartship(TYPE_5),
         Gain(TYPE_5),
         SaCorrection(TYPE_5);

         private final String type;

         PerChannel(String type) {
            this.type = type;
         }

         @Override
         public String getName(String channelId) {
            return "TransceiverMgr/" + channelId + "/" + name();
         }

         @Override
         public String getType() {
            return type;
         }
      }
   }

   public static final class ProcessingMgr {
      private ProcessingMgr() {
      }

      public static final class ChannelProcessingCommon {
         private ChannelProcessingCommon() {
         }

         public enum PerChannel implements PerChannelParameter {
            NoiseEstimate(TYPE_5);

            private final String type;

            PerChannel(String type) {
               this.type = type;
            }

            @Override
            public String getName(String channelId) {
               return "ProcessingMgr/" + channelId + "/ChannelProcessingCommon/" + name();
            }

            @Override
            public String getType() {
               return type;
            }
         }
      }
   }
}
