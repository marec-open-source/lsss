package no.imr.lsss.modules.trawl.spd;

final class VLine extends BaseLine {
   final String fishNo;
   final char weightVolumeCode;
   final String weightVolume;
   final char lengthUnit;
   final String length;
   final char fat;
   final char sex;
   final char state;
   final String specialState;
   final char stomach;
   final char digest;
   final char liver;
   final char parasite;
   final String specialCode;
   final String vertebra;
   final String age;
   final String spawnAge;
   final String spawnZone;
   final char otolithRead;
   final char otolithType;
   final char shellEdge;
   final char shellKernel;
   final String calibration;
   final String growZone;
   final char marking;
   final String serialCode;
   final String markNo;
   final char weightVol;
   final String gonadQuantity;
   final String liverQuantity;
   final String netWeightVol;

   VLine(String line) {
      super('V', line);

      fishNo = substring(line, 40, 43);
      weightVolumeCode = charAt(line, 43);
      weightVolume = substring(line, 44, 49);
      lengthUnit = charAt(line, 49);
      length = substring(line, 50, 53);
      fat = charAt(line, 53);
      sex = charAt(line, 54);
      state = charAt(line, 55);
      specialState = substring(line, 56, 58);
      stomach = charAt(line, 58);
      digest = charAt(line, 59);
      liver = charAt(line, 60);
      parasite = charAt(line, 61);
      specialCode = substring(line, 62, 66);
      vertebra = substring(line, 66, 68);
      age = substring(line, 68, 30 + 40);
      spawnAge = substring(line, 70, 72);
      spawnZone = substring(line, 72, 74);
      otolithRead = charAt(line, 74);
      otolithType = charAt(line, 75);
      shellEdge = charAt(line, 76);
      shellKernel = charAt(line, 77);
      calibration = substring(line, 78, 80);
      growZone = substring(line, 80, 100);
      marking = charAt(line, 100);
      serialCode = substring(line, 101, 103);
      markNo = substring(line, 103, 109);
      weightVol = charAt(line, 109);
      gonadQuantity = substring(line, 110, 114);
      liverQuantity = substring(line, 114, 118);
      netWeightVol = substring(line, 118, 123);
   }
}
