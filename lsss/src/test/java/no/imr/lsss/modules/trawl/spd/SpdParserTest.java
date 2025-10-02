package no.imr.lsss.modules.trawl.spd;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SpdParserTest {
   @Test
   void spd() throws IOException {
      List<SpdStation> stations = SpdTestUtils.loadStations();

      assertEquals(7, stations.size());

      SpdStation station = stations.get(5);
      SLine sLine = station.sLine();
      assertEquals(2018, sLine.year);               //   1-4
      assertEquals(58, sLine.country);              //   5-6
      assertEquals('4', sLine.shipCode);            //   7
      assertEquals("JH    ", sLine.shipName);       //   8-13
      assertEquals(7, sLine.month);                 //  14-15
      assertEquals(3, sLine.day);                   //  16-17
      assertEquals(336, sLine.stationNo);           //  18-21
      assertEquals(24106, sLine.serialNo);          //  22-26
      assertEquals(' ', sLine.stationType);         //  27
      assertEquals("56394", sLine.latitude);        //  28-32
      assertEquals(" 05094", sLine.longitude);      //  33-38
      assertEquals('0', sLine.nsew);                //  39
      assertEquals('2', sLine.system);              //  40
      assertEquals("41", sLine.territory);          //  41-42
      assertEquals(" 65", sLine.location);          //  43-45
      assertEquals("  61", sLine.bottomDepth);      //  46-49
      assertEquals(" 1", sLine.noOfTools);          //  50-51
      assertEquals("    ", sLine.toolCode);         //  52-55
      assertEquals("16", sLine.toolNumber);         //  56-57
      assertEquals("31", sLine.direction);          //  58-59
      assertEquals("  ", sLine.speed);              //  60-61
      assertEquals("2122", sLine.startTime);        //  62-65
      assertEquals(545.0, sLine.startLog);          //  66-69
      assertEquals("2152", sLine.stopTime);         //  70-73
      assertEquals(19.0, sLine.distance);           //  74-76
      assertEquals('1', sLine.state);               //  77
      assertEquals('1', sLine.quality);             //  78
      assertEquals("62", sLine.maxFishDepth);       //  79-82
      assertEquals("61", sLine.minFishDepth);       //  83-86
      assertEquals("-10", sLine.trawlOpening);      //  87-89
      assertEquals("  ", sLine.stdDevTrawlOpening); //  90-91
      assertEquals(" 45", sLine.doorSpread);        //  92-94
      assertEquals("   ", sLine.stdDevDoorSpread);  //  95-97
      assertEquals("  ", sLine.specialCode);        //  98-99
      assertEquals(" 100", sLine.wireLength);       // 100-103
      assertEquals(' ', sLine.qualityMark);         // 122
      assertEquals("10", sLine.qualityProc);        // 123-124

      SpdTarget target = station.targets().get(6);
      TLine tLine = target.tLine();
      assertEquals('2', tLine.specieCode);                // 27
      assertEquals("MAKRELL     ", tLine.speciesName);     // 28-39
      assertEquals(1, tLine.speciePartNo);                // 40
      assertEquals("34", tLine.sampleType);               // 41-42
      assertEquals("  ", tLine.group);                    // 43-44
      assertEquals('1', tLine.conservation);              // 45
      assertEquals('1', tLine.measure);                   // 46
      assertEquals(203, tLine.catchQuantum);              // 47-53
      assertEquals(2, tLine.catchNumber);                 // 54-59
      assertEquals('1', tLine.target);                    // 60
      assertEquals('F', tLine.lengthUnit);                // 61
      assertEquals("   203", tLine.weightOfLengthSample); // 62-67
      assertEquals(2, tLine.lengthSampleNo);              // 68-71
      assertEquals("   2", tLine.individualSampleNo);     // 72-75
      assertEquals(' ', tLine.otolithShell);              // 76
      assertEquals(' ', tLine.parasite);                  // 77
      assertEquals(' ', tLine.stomach);                   // 78
      assertEquals(' ', tLine.genetics);                  // 79

      ULine uLine = target.uLines().getFirst();
      assertEquals('2', uLine.interval);       // 41
      assertEquals(' ', uLine.sex);            // 42
      assertEquals(190, uLine.minLengthGroup); // 43-45
      assertArrayEquals(new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, uLine.getFishCounts()); // 46-119

      VLine vLine = target.vLines().getFirst();
      assertEquals("  1", vLine.fishNo);         // 41-43
      assertEquals('1', vLine.weightVolumeCode); // 44
      assertEquals("   50", vLine.weightVolume); // 45-49
      assertEquals('2', vLine.lengthUnit);       // 50
      assertEquals("190", vLine.length);         // 51-53
      assertEquals(' ', vLine.fat);              // 54
      assertEquals(' ', vLine.sex);              // 55
      assertEquals(' ', vLine.state);            // 56
      assertEquals("", vLine.specialState);      // 57-58
      assertEquals(' ', vLine.stomach);          // 59
      assertEquals(' ', vLine.digest);           // 60
      assertEquals(' ', vLine.liver);            // 61
      assertEquals(' ', vLine.parasite);         // 62
      assertEquals("", vLine.specialCode);       // 63-66
      assertEquals("", vLine.vertebra);          // 67-68
      assertEquals("", vLine.age);               // 69-70
      assertEquals("", vLine.spawnAge);          // 71-72
      assertEquals("", vLine.spawnZone);         // 73-74
      assertEquals(' ', vLine.otolithRead);      // 75
      assertEquals(' ', vLine.otolithType);      // 76
      assertEquals(' ', vLine.shellEdge);        // 77
      assertEquals(' ', vLine.shellKernel);      // 78
      assertEquals("", vLine.calibration);       // 79-80
      assertEquals("", vLine.growZone);          // 81-100
      assertEquals(' ', vLine.marking);          // 101
      assertEquals("", vLine.serialCode);        // 102-103
      assertEquals("", vLine.markNo);            // 104-109
      assertEquals(' ', vLine.weightVol);        // 110
      assertEquals("", vLine.gonadQuantity);     // 111-114
      assertEquals("", vLine.liverQuantity);     // 115-118
      assertEquals("", vLine.netWeightVol);      // 119-123

      // ---

      target = stations.get(6).targets().get(5);
      assertEquals("MAKRELL     ", tLine.speciesName);
      assertArrayEquals(new int[]{2, 2, 7, 10, 9, 5, 2, 1, 1, 0, 0, 0, 0, 0, 3, 7, 14, 13, 11, 7, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1},
            target.uLines().getFirst().getFishCounts());
   }
}
