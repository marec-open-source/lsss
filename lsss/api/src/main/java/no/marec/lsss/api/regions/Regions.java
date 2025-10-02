package no.marec.lsss.api.regions;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.Ping;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.util.FloatRange;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A collection of {@link Region}s.
 */
@DoNotImplement
public interface Regions {
   /**
    * {@return an observable for the selected regions}
    */
   ObservableValue<? extends Collection<? extends Region>> selected();

   /**
    * Replaces the selected regions.
    *
    * @param regions the regions to be selected
    */
   void setSelected(Collection<? extends Region> regions);

   /**
    * {@return an observable for regions that are modified}
    */
   Observable<? extends RegionChangeEvent> changed();

   /**
    * {@return an observable for regions that are deleted}
    */
   Observable<? extends Collection<? extends Region>> deleted();

   /**
    * {@return an observable for regions with modified labels}
    */
   Observable<? extends Region> labelsChanged();

   /**
    * Gets the effective depth intervals for a region.
    * <p>
    * This takes into account:
    * <ul>
    *    <li>Schools on top of layers</li>
    *    <li>Ping interval exclusions</li>
    *    <li>Depth dependent deletion</li>
    * </ul>
    *
    * @param region  a region
    * @param ping    a ping
    * @param channel a channel
    * @return a list of depth ranges
    */
   List<? extends FloatRange> depthRanges(Region region, Ping ping, int channel);

   /**
    * Should be called after editing one or more {@link Interpretation}.
    */
   void finishedEditingInterpretations();

   /**
    * {@return an observable for changed interpretation}
    */
   Observable<?> interpretationChanged();

   /**
    * Creates a new school.
    *
    * @param mask the school mask as a map from ping index to depth ranges
    * @return the new school, or {@code null} if a school could not be created
    */
   @Nullable Region createSchool(Map<PingIndex, List<FloatRange>> mask);
}
