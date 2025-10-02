package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;

/**
 * Functions implemented by submodules doing pixel categorization.
 */
interface Categorizer {
   void categorize(Pixel pixel, PerPingAPriori perPingAPriori);
}
