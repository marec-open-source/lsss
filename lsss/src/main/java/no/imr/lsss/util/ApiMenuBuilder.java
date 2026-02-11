package no.imr.lsss.util;

import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.reports.ReportGenerator;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.application.DatabaseConf;
import no.imr.lsss.framework.config.application.LsssServerConf;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.modules.ModuleConf;
import no.imr.lsss.framework.config.survey.modules.ModuleConfigurationUnit;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.echogram.ColorBarModule;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.parameter.BaseValueParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.Point;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class ApiMenuBuilder {
   private final LSSS lsss;
   private final @Nullable Component referenceComponent;
   private final String baseUrl;
   private final JMenu menu;

   private ApiMenuBuilder(String baseUrl, String name, LSSS lsss, @Nullable Component referenceComponent) {
      this.lsss = lsss;
      this.referenceComponent = referenceComponent;
      this.baseUrl = baseUrl;
      menu = MenuUtils.multiColumn(new JMenu(name));
   }

   public ApiMenuBuilder(LSSS lsss, @Nullable Component referenceComponent) {
      this(lsss.getConfigurationManager().getAppMiscConf().getLsssServerConf().getServerBaseUri(), "API", lsss, referenceComponent);

      LsssIcons.LSSS_SERVER.on(menu);
   }

   private ApiMenuBuilder separator() {
      menu.addSeparator();
      return this;
   }

   private ApiMenuBuilder subMenu(String path, Consumer<ApiMenuBuilder> consumer) {
      ApiMenuBuilder subBuilder = new ApiMenuBuilder(baseUrl + path, path, lsss, referenceComponent);
      consumer.accept(subBuilder);
      menu.add(subBuilder.menu);
      return this;
   }

   private ApiMenuBuilder item(String path) {
      return item(path, true);
   }

   private ApiMenuBuilder item(String path, boolean enabled) {
      JMenuItem item = menu.add(path);
      item.setEnabled(enabled);
      String url = baseUrl + path;
      item.addActionListener(_ -> openUrl(url));
      item.setToolTipText(url);
      return this;
   }

   private void openUrl(String url) {
      ConfigurationManager configurationManager = lsss.getConfigurationManager();
      LsssServerConf lsssServerConf = configurationManager.getAppMiscConf().getLsssServerConf();
      if (!lsssServerConf.getLsssServerPluginEnabled()) {
         JOptionPane.showMessageDialog(referenceComponent, "The LSSS server plugin is not enabled.");
         configurationManager.getApplicationConfiguration().getPluginConf().showInConfigurationDialog();
         return;
      }
      if (!lsssServerConf.serverActive.getBooleanValue()) {
         JOptionPane.showMessageDialog(referenceComponent, "The LSSS server is not started.");
         lsssServerConf.showInConfigurationDialog();
         return;
      }
      GuiUtils.desktopBrowse(URI.create(url), referenceComponent);
   }

   public void addTo(JPopupMenu popupMenu) {
      popupMenu.add(menu);

      JMenuItem docItem = MiscIcons.HELP.on(menu.add("Documentation"));
      String url = baseUrl + "/lsss/doc/commands.html";
      docItem.addActionListener(_ -> openUrl(url));
      docItem.setToolTipText(url);
   }

   public ApiMenuBuilder configurationUnitMenu(ConfigurationUnit configurationUnit) {
      return switch (configurationUnit) {
         case ModuleConf _ -> {
            yield item("/lsss/module");
         }
         case ModuleConfigurationUnit moduleConfigurationUnit -> {
            BaseLsssModule module = moduleConfigurationUnit.getModule();
            if (module instanceof BaseModuleOverlay overlay) {
               yield overlayMenu(overlay);
            } else {
               yield moduleMenu(module);
            }
         }
         default -> {
            boolean isAppConfig = configurationUnit.getConfigurationManager().getApplicationConfiguration().getAllUnitsRecursively()
                  .anyMatch(configurationUnit::equals);
            String persistentName = configurationUnit.getPersistentName();
            subMenu("/lsss/" + (isAppConfig ? "application" : "survey") + "/config/unit/" + persistentName + "/", unitBuilder -> {
               unitBuilder
                     .item("parameter")
                     .parameterMenu("parameter/", configurationUnit.getParameterCollection().getSubConfigurables())
                     .item("xml");
               unitBuilder.applicationConfigSpecificItems(configurationUnit);
               unitBuilder.surveyConfigSpecificItems(configurationUnit);
            });
            if (configurationUnit instanceof DatabaseConf) {
               databaseMenu();
            }
            yield this;
         }
      };
   }

   private void applicationConfigSpecificItems(ConfigurationUnit configurationUnit) {
      if (configurationUnit instanceof DatabaseConf) {
         separator()
               .item("connected");
      }
   }

   private void dataMenu(@Nullable EchogramPoint echogramPoint) {
      subMenu("/lsss/data/", dataBuilder -> {
         dataBuilder
               .item("config")
               .item("frequencies")
               .item("frequency")
               .item("mode");
         if (echogramPoint != null) {
            dataBuilder
                  .item("ping?all=true&pingNumber=" + echogramPoint.pingIndex().getPingNumber());
         }
      });
   }

   public ApiMenuBuilder moduleMenu(BaseLsssModule module) {
      return moduleMenu(module, allOverlays(module), null);
   }

   public ApiMenuBuilder moduleMenu(BaseLsssModule module, List<? extends BaseModuleOverlay> overlays, @Nullable Point point) {
      subMenu("/lsss/module/" + module.getPersistentName() + "/", moduleBuilder -> {
         moduleBuilder
               .subMenu("config/", configBuilder -> {
                  configBuilder
                        .item("parameter")
                        .parameterMenu("parameter/", module.getConfigurable().getSubConfigurables())
                        .item("xml");
               })
               .item("data", module instanceof PojoDataContainer && module.isEnabled())
               .item("docked", module instanceof BaseViewModule viewModule && viewModule.getFloatableInfo() != null)
               .item("image", module instanceof BaseViewModule && module.isEnabled());
         if (module instanceof BaseOverlaidModule<?>) {
            moduleBuilder
                  .item("overlay");
            if (!overlays.isEmpty()) {
               moduleBuilder
                     .subMenu("overlay/", overlaysBuilder -> {
                        overlays.stream()
                              .sorted(Comparator.comparing(BaseLsssModule::getPersistentName))
                              .forEach(overlay -> overlaysBuilder.overlayMenu("", overlay));
                     });
            }
         }
         moduleBuilder.moduleSpecificItems(module);
      });
      switch (module) {
         case EchogramModule echogramModule -> {
            EchogramPoint echogramPoint = point != null ? echogramModule.imagePointToEchogramPoint(point) : null;
            dataMenu(echogramPoint);
            Region region = echogramPoint != null ? echogramModule.getLSSS().getRegionManager().getRegion(echogramPoint) : null;
            regionsMenu(region);
         }
         case InterpretationModule _ -> {
            databaseMenu();
         }
         default -> {
         }
      }
      return this;
   }

   public ApiMenuBuilder overlayMenu(BaseModuleOverlay overlay) {
      return overlayMenu("/lsss/module/" + overlay.getOverlaidModule().getPersistentName() + "/overlay/", overlay);
   }

   private ApiMenuBuilder overlayMenu(String pathPrefix, BaseModuleOverlay overlay) {
      return subMenu(pathPrefix + overlay.getPersistentName() + "/", overlayBuilder -> {
         overlayBuilder
               .subMenu("config/", configBuilder -> {
                  configBuilder
                        .item("parameter")
                        .parameterMenu("parameter/", overlay.getConfigurable().getSubConfigurables())
                        .item("xml");
               })
               .item("data", overlay instanceof PojoDataContainer && overlay.isEnabled())
               .item("enabled")
               .item("image");
      });
   }

   private void moduleSpecificItems(BaseLsssModule module) {
      switch (module) {
         case ColorBarModule _ -> {
            separator()
                  .item("colormap")
                  .item("colormaps")
                  .subMenu("threshold/", thresholdBuilder -> {
                     thresholdBuilder
                           .item("max")
                           .item("min");
                  });
         }
         case EchogramModule _ -> {
            separator()
                  .item("current-echogram-point")
                  .item("zoom")
                  .subMenu("zoom/", zoomBuilder -> {
                     zoomBuilder
                           .item("max");
                  });
         }
         default -> {
         }
      }
   }

   private void databaseMenu() {
      boolean hasSurvey = lsss.getConfigurationManager().getSurveyConf().getSurvey() != null;
      subMenu("/lsss/database/", databaseBuilder -> {
         databaseBuilder
               .item("report", hasSurvey)
               .subMenu("report/", reportBuilder -> {
                  for (int report : ReportGenerator.REPORTS_TIME) {
                     reportBuilder
                           .item(Integer.toString(report), hasSurvey);
                  }
               });
      });
   }

   private void regionsMenu(@Nullable Region region) {
      subMenu("/lsss/regions/", regionsBuilder -> {
         regionsBuilder
               .item("deletion")
               .item("exclusion")
               .item("region");
         if (region != null) {
            regionsBuilder
                  .subMenu("region/", regionBuilder -> {
                     regionBuilder
                           .item(String.valueOf(region.getObjectNumber()))
                           .subMenu(region.getObjectNumber() + "/", objectNumberBuilder -> {
                              objectNumberBuilder
                                    .item("mask", region instanceof School)
                                    .item("scrutiny");
                           });
                  });
         }
         regionsBuilder
               .item("selection");
      });
   }

   private void surveyConfigSpecificItems(ConfigurationUnit configurationUnit) {
      switch (configurationUnit) {
         case AcousticCategoryConf _ -> {
            separator()
                  .item("category");
         }
         case DataConf _ -> {
            separator()
                  .item("files")
                  .subMenu("files/", filesBuilder -> {
                     filesBuilder
                           .item("selection");
                  });
         }
         default -> {
         }
      }
   }

   public static List<? extends BaseModuleOverlay> allOverlays(BaseLsssModule module) {
      return module instanceof BaseOverlaidModule<?> overlaidModule
            ? overlaidModule.userVisibleForegroundOverlays().toList()
            : List.of();
   }

   private ApiMenuBuilder parameterMenu(String path, Collection<? extends Configurable> configurables) {
      if (configurables.isEmpty()) {
         return this;
      }
      return subMenu(path, parameterMenuBuilder -> {
         for (Configurable configurable : configurables) {
            parameterMenuBuilder.parameterMenuItem(configurable);
         }
      });
   }

   private void parameterMenuItem(Configurable configurable) {
      if (configurable instanceof BaseValueParameter<?> || configurable instanceof ButtonParameter) {
         item(configurable.getName().persistentName());
      } else {
         parameterMenu(configurable.getName().persistentName() + "/", configurable.getSubConfigurables());
      }
   }
}
