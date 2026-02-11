package no.imr.korona.apps.fiskview;

import no.imr.korona.Korona;
import no.imr.korona.config.KoronaSettingsUtils;
import no.imr.korona.config.gui.ConfigFileSettingsEditor;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

/**
 * The menu of {@link FiskView}.
 */
final class FiskViewMenu {
   private final FiskView fiskView;
   private final JMenuBar menuBar = new JMenuBar();

   FiskViewMenu(FiskView fiskView) {
      this.fiskView = fiskView;

      menuBar.add(createFileMenu());
      menuBar.add(createHelpMenu());
   }

   JMenuBar getMenuBar() {
      return menuBar;
   }

   private JMenu createFileMenu() {
      JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);

      JMenuItem loadMenuItem = MiscIcons.OPEN.on(fileMenu.add("Open raw file..."));
      loadMenuItem.setMnemonic(KeyEvent.VK_O);
      loadMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
      loadMenuItem.addActionListener(_ -> fiskView.loadRawFile());

      fileMenu.addSeparator();

      JMenuItem loadDescMenuItem = fileMenu.add("Load module configuration...");
      loadDescMenuItem.setMnemonic(KeyEvent.VK_M);
      loadDescMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK));
      loadDescMenuItem.addActionListener(_ -> fiskView.loadCdsFile());

      JMenuItem saveDescMenuItem = fileMenu.add("Save configuration");
      saveDescMenuItem.setMnemonic(KeyEvent.VK_S);
      saveDescMenuItem.addActionListener(_ -> fiskView.saveCdsFile());

      JMenuItem saveAsDescMenuItem = fileMenu.add("Save configuration as...");
      saveAsDescMenuItem.setMnemonic(KeyEvent.VK_A);
      saveAsDescMenuItem.addActionListener(_ -> fiskView.saveCdsFileAs());

      JMenuItem editDescMenuItem = fileMenu.add("Edit configuration...");
      editDescMenuItem.setMnemonic(KeyEvent.VK_E);
      editDescMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
      editDescMenuItem.addActionListener(_ -> fiskView.editCurrentConfiguration());

      JMenuItem newDescMenuItem = fileMenu.add("New configuration...");
      newDescMenuItem.setMnemonic(KeyEvent.VK_N);
      newDescMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
      newDescMenuItem.addActionListener(_ -> {
         if (fiskView.isCdsUnmodifiedOrUserApproved()) {
            fiskView.createNewConfiguration();
         }
      });

      JMenuItem removeDescMenuItem = MiscIcons.DELETE.on(fileMenu.add("Remove all modules"));
      removeDescMenuItem.setMnemonic(KeyEvent.VK_R);
      removeDescMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
      removeDescMenuItem.addActionListener(_ -> {
         if (fiskView.isCdsUnmodifiedOrUserApproved()) {
            fiskView.removeConfiguration();
         }
      });

      fileMenu.addSeparator();

      JMenuItem editMainConfigDirItem = fileMenu.add("Edit main config directory...");
      editMainConfigDirItem.addActionListener(_ -> {
         KoronaSettingsUtils.showMainConfigDirDialog(fiskView.getKorona(), fiskView.getFrame(), Path.of(""));
      });

      fileMenu.addSeparator();

      fileMenu.add(createConfigFileSettingsMenu());

      fileMenu.addSeparator();

      JMenuItem saveAllItem = MiscIcons.SAVE.on(fileMenu.add("Save all changes"));
      saveAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
      saveAllItem.addActionListener(_ -> fiskView.saveAll());

      fileMenu.addSeparator();

      JMenuItem exitItem = MiscIcons.POWER.on(fileMenu.add("Exit"));
      exitItem.setMnemonic(KeyEvent.VK_X);
      exitItem.addActionListener(_ -> fiskView.shutDown());

      return fileMenu;
   }

   private JMenu createConfigFileSettingsMenu() {
      JMenu menu = new JMenu("Config file settings");
      menu.setMnemonic(KeyEvent.VK_C);

      JMenuItem cfsLoadItem = MiscIcons.OPEN.on(menu.add("Load..."));
      cfsLoadItem.setMnemonic(KeyEvent.VK_L);
      cfsLoadItem.addActionListener(_ -> fiskView.loadCfsFile());

      JMenuItem cfsSaveItem = MiscIcons.SAVE.on(menu.add("Save"));
      cfsSaveItem.setMnemonic(KeyEvent.VK_S);
      cfsSaveItem.addActionListener(_ -> fiskView.saveCfsFile());

      JMenuItem cfsSaveAsItem = menu.add("Save as...");
      cfsSaveAsItem.setMnemonic(KeyEvent.VK_A);
      cfsSaveAsItem.addActionListener(_ -> fiskView.saveCfsFileAs());

      JMenuItem cfsEditItem = MiscIcons.SETTINGS.on(menu.add("Edit..."));
      cfsEditItem.setMnemonic(KeyEvent.VK_E);
      cfsEditItem.addActionListener(_ -> {
         ConfigFileSettingsEditor.showDialog(fiskView.getModuleContainer().getConfigFileSettings(), fiskView.getFrame(),
               true, ContextVisibility.SHOW, KoronaHelp.CONFIG_FILE_SETTINGS);
      });

      return menu;
   }

   private JMenu createHelpMenu() {
      JMenu menu = new JMenu("Help");
      menu.setMnemonic(KeyEvent.VK_H);

      menu.add(fiskView.getKoronaHelpSystem().createHelpMenuItem());
      menu.add(MenuItems.about(Korona.APPLICATION_INFO));

      return menu;
   }
}
