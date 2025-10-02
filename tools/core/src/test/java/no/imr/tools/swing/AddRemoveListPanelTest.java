package no.imr.tools.swing;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class AddRemoveListPanelTest {
   @Test
   void test1() {
      List<Integer> all = Lists.newArrayList(5, 4, 3, 2, 1);
      List<Integer> sel = Lists.newArrayList(9);

      AddRemoveListPanel<Integer> panel = new AddRemoveListPanel<>(all, sel, "test case");

      panel.getAllOptionsJList().setSelectedIndex(0);
      panel.getAddButton().doClick();
      assertEquals(9, (int) sel.get(0));
      assertEquals(5, (int) sel.get(1));
   }
}
