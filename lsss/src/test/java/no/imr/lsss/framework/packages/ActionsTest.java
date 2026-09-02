package no.imr.lsss.framework.packages;

import no.imr.lsss.LSSS;
import no.imr.lsss.test.LsssTestUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

final class ActionsTest {
   @Test
   void allActionsRegisteredToLsssPackage() {
      LSSS lsss = LsssTestUtils.start();
      LsssPackage lsssPackage = lsss.getPackageManager().lsssPackage;
      for (Field field : Actions.class.getFields()) {
         if (LsssAction.class.isAssignableFrom(field.getType())) {
            LsssAction action = lsssPackage.getAction(field.getName());
            assertNotNull(action, field.getName());
            assertEquals(field.getName(), action.getId());
         }
      }
      lsss.close();
   }
}
