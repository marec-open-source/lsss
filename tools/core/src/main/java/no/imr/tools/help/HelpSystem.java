package no.imr.tools.help;

import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class HelpSystem implements AutoCloseable {
   private final HelpSystemInfo helpSystemInfo;
   private final List<HelpSystemHelpSet> helpSets = new ArrayList<>();
   private @Nullable HelpDisplayer helpDisplayer;

   public HelpSystem(HelpSystemInfo helpSystemInfo) {
      this.helpSystemInfo = helpSystemInfo;
   }

   public void addHelpSet(HelpSystemHelpSet helpSet) {
      if (helpSet.getHelpDir() == null) {
         return;
      }
      helpSet.setHelpSystem(this);
      helpSets.add(helpSet);
   }

   public List<HelpSystemHelpSet> getHelpSets() {
      return helpSets;
   }

   public HelpSystemHelpSet getMainHelpSet() {
      return helpSets.isEmpty() ? HelpSystemHelpSet.EMPTY : helpSets.getFirst();
   }

   public HelpSystemInfo getHelpSystemInfo() {
      return helpSystemInfo;
   }

   synchronized HelpDisplayer getHelpDisplayer() {
      if (helpDisplayer == null) {
         helpDisplayer = ServiceLoader.load(HelpDisplayerService.class)
               .findFirst()
               .map(service -> service.createHelpSystemDisplayer(this))
               .orElseGet(HelpDisplayer::noneAvailable);
      }
      return helpDisplayer;
   }

   @Override
   public synchronized void close() {
      if (helpDisplayer != null) {
         helpDisplayer.close();
      }
   }

   public JMenuItem createHelpMenuItem() {
      JMenuItem item = MiscIcons.HELP.on(new JMenuItem("Help"));
      item.setMnemonic(KeyEvent.VK_H);
      getMainHelpSet().getTopHelpID().enableHelpKeyMenuItem(item);
      return item;
   }
}
