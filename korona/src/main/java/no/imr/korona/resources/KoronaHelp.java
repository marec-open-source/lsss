package no.imr.korona.resources;

import no.imr.tools.help.HelpID;
import no.imr.tools.help.HelpSystemHelpSet;

public final class KoronaHelp {
   public static final HelpSystemHelpSet HELP_SET = new HelpSystemHelpSet("no/imr/korona/resources/help", "korona");

   public static final HelpID BROADBAND_NOTCH_FILTERS = HELP_SET.createHelpID("BroadbandNotchFilters");
   public static final HelpID BROADBAND_SPLITTER_BANDS = HELP_SET.createHelpID("BroadbandSplitterBands");
   public static final HelpID CATEGORIZATION_LIBRARY = HELP_SET.createHelpID("CategorizationLibrary");
   public static final HelpID CATEGORIZATION_LIBRARY_VIEW_MISCLASSIFICATION = HELP_SET.createHelpID("CategorizationLibrary/View-Misclassification");
   public static final HelpID CONFIG_FILE_SETTINGS = HELP_SET.createHelpID("ConfigFileSettings");
   public static final HelpID KORONA_PLAYBOX = HELP_SET.createHelpID("KoronaPlaybox");
   public static final HelpID MODULE_CONFIGURATION = HELP_SET.createHelpID("ModuleConfiguration");
   public static final HelpID PLANKTON_CONFIGURATION = HELP_SET.createHelpID("PlanktonConfiguration");

   private KoronaHelp() {
   }
}
