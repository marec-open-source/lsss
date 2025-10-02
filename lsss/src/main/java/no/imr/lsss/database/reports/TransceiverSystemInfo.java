package no.imr.lsss.database.reports;

final class TransceiverSystemInfo {
   // Set system info for each EK GPT/WBT/.. channel:
   private int mFrequency;
   private String mTransducerLocation = "";
   private String mTransducerManufacturer = "";
   private String mTransducerModel = "";
   private String mTransducerSerial = "";
   private String mTransducerBeamType = "";
   private double mTransducerDepth;
   private double mTransducerOrientationElevation;
   private double mTransducerOrientationAzimuth;
   private double mTransducerOrientationRotation;
   private double mTransducerPSI;
   private double mTransducerBeamAngleMajor;
   private double mTransducerBeamAngleMinor;
   private String mTransceiverManufacturer = "";
   private String mTransceiverModel = "";
   private String mTransceiverSerial = "";
   private String mTransceiverFirmware = "";

   TransceiverSystemInfo() {
   }

   void setFrequency(int aFrequency) {
      mFrequency = aFrequency;
   }

   void setTransducerLocation(String aTransducerLocation) {
      mTransducerLocation = aTransducerLocation;
   }

   void setTransducerManufacturer(String aTransducerManufacturer) {
      mTransducerManufacturer = aTransducerManufacturer;
   }

   void setTransducerModel(String aTransducerModel) {
      mTransducerModel = aTransducerModel;
   }

   void setTransducerSerial(String aTransducerSerial) {
      mTransducerSerial = aTransducerSerial;
   }

   void setTransducerBeamType(String aTransducerBeamType) {
      mTransducerBeamType = aTransducerBeamType;
   }

   void setTransducerDepth(double aTransducerDepth) {
      mTransducerDepth = aTransducerDepth;
   }

   void setTransducerOrientationElevation(double aTransducerOrientationElevation) {
      mTransducerOrientationElevation = aTransducerOrientationElevation;
   }

   void setTransducerOrientationAzimuth(double aTransducerOrientationAzimuth) {
      mTransducerOrientationAzimuth = aTransducerOrientationAzimuth;
   }

   void setTransducerOrientationRotation(double aTransducerOrientationRotation) {
      mTransducerOrientationRotation = aTransducerOrientationRotation;
   }

   void setTransducerPSI(double aTransducerPSI) {
      mTransducerPSI = aTransducerPSI;
   }

   void setTransducerBeamAngleMajor(double aTransducerBeamAngleMajor) {
      mTransducerBeamAngleMajor = aTransducerBeamAngleMajor;
   }

   void setTransducerBeamAngleMinor(double aTransducerBeamAngleMinor) {
      mTransducerBeamAngleMinor = aTransducerBeamAngleMinor;
   }

   void setTransceiverManufacturer(String aTransceiverManufacturer) {
      mTransceiverManufacturer = aTransceiverManufacturer;
   }

   void setTransceiverModel(String aTransceiverModel) {
      mTransceiverModel = aTransceiverModel;
   }

   void setTransceiverSerial(String aTransceiverSerial) {
      mTransceiverSerial = aTransceiverSerial;
   }

   void setTransceiverFirmware(String aTransceiverFirmware) {
      mTransceiverFirmware = aTransceiverFirmware;
   }

   int getFrequency() {
      return mFrequency;
   }

   String getTransducerLocation() {
      return mTransducerLocation;
   }

   String getTransducerManufacturer() {
      return mTransducerManufacturer;
   }

   String getTransducerModel() {
      return mTransducerModel;
   }

   String getTransducerSerial() {
      return mTransducerSerial;
   }

   String getTransducerBeamType() {
      return mTransducerBeamType;
   }

   double getTransducerDepth() {
      return mTransducerDepth;
   }

   double getTransducerOrientationElevation() {
      return mTransducerOrientationElevation;
   }

   double getTransducerOrientationAzimuth() {
      return mTransducerOrientationAzimuth;
   }

   double getTransducerOrientationRotation() {
      return mTransducerOrientationRotation;
   }

   double getTransducerPSI() {
      return mTransducerPSI;
   }

   double getTransducerBeamAngleMajor() {
      return mTransducerBeamAngleMajor;
   }

   double getTransducerBeamAngleMinor() {
      return mTransducerBeamAngleMinor;
   }

   String getTransceiverManufacturer() {
      return mTransceiverManufacturer;
   }

   String getTransceiverModel() {
      return mTransceiverModel;
   }

   String getTransceiverSerial() {
      return mTransceiverSerial;
   }

   String getTransceiverFirmware() {
      return mTransceiverFirmware;
   }
}
