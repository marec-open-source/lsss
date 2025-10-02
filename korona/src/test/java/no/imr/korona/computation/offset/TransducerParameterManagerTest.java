package no.imr.korona.computation.offset;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class TransducerParameterManagerTest {
   @Test
   void testRange() {
      TransducerParameterManager manager = new TransducerParameterManager(TransducerParameters.ParameterType.RANGE);
      manager.getTransducerParametersList().add(makeTransducerParameters(10, 5, 100));
      manager.getTransducerParametersList().add(makeTransducerParameters(20, 6, 90));
      manager.getTransducerParametersList().add(makeTransducerParameters(30, 8, 50));
      manager.sortAndNotify();

      assertEquals(Optional.empty(), manager.getTransducerOffsetPar(9));
      assertEquals(10, manager.getTransducerOffsetPar(10).orElseThrow().getKHz());
      assertEquals(Optional.empty(), manager.getTransducerOffsetPar(15));
      assertEquals(20, manager.getTransducerOffsetPar(20).orElseThrow().getKHz());
      assertEquals(30, manager.getTransducerOffsetPar(30).orElseThrow().getKHz());
      assertEquals(Optional.empty(), manager.getTransducerOffsetPar(31));

      assertEquals(Optional.empty(), manager.getBlindZone(9));
      assertEquals(Optional.of(5f), manager.getBlindZone(10));
      assertEquals(Optional.of(6f), manager.getBlindZone(14));
      assertEquals(Optional.of(6f), manager.getBlindZone(20));
      assertEquals(Optional.of(8f), manager.getBlindZone(28));
      assertEquals(Optional.of(8f), manager.getBlindZone(30));
      assertEquals(Optional.empty(), manager.getBlindZone(31));

      assertEquals(Optional.empty(), manager.getRange(9));
      assertEquals(Optional.of(100f), manager.getRange(10));
      assertEquals(Optional.of(99f), manager.getRange(11));
      assertEquals(Optional.of(93f), manager.getRange(17));
      assertEquals(Optional.of(90f), manager.getRange(20));
      assertEquals(Optional.of(70f), manager.getRange(25));
      assertEquals(Optional.of(50f), manager.getRange(30));
      assertEquals(Optional.empty(), manager.getRange(31));
   }

   private static TransducerParameters makeTransducerParameters(int kHz, int blindZone, int range) {
      TransducerParameters transducerParameters = new TransducerParameters(TransducerParameters.ParameterType.RANGE);
      transducerParameters.frequency.setIntValue(kHz);
      transducerParameters.blindZone.setFloatValue(blindZone);
      transducerParameters.range.setFloatValue(range);
      return transducerParameters;
   }
}
