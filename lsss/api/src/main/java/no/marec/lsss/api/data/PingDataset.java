package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * A dataset consisting of a sequence of {@link Ping}.
 * <p>
 * The duration of a ping starts at the time of the corresponding ping index,
 * and ends at the time of the next ping index.
 */
@DoNotImplement
public interface PingDataset {
   /**
    * {@return the ping configuration for this dataset}
    */
   PingConfiguration getPingConfiguration();

   /**
    * {@return the range of all pings in the dataset}
    * <p>
    * The end of this range is one step beyond the last ping in this dataset.
    */
   PingRange getTotalPingRange();

   /**
    * Finds the ping index with a specified ping number.
    *
    * @param pingNumber a ping number
    * @return the corresponding ping index, or {@code null} is none exists
    */
   @Nullable PingIndex getPingIndex(long pingNumber);

   /**
    * Finds the ping index containing a specified time.
    * <p>
    * This will not be the end of {@link #getTotalPingRange()}, unless this dataset is empty.
    *
    * @param instant a time
    * @return the ping index containing that time, or {@code null} is none exists
    */
   @Nullable PingIndex getContainingPingIndex(Instant instant);

   /**
    * Finds the ping index closest to a specified time.
    * <p>
    * This might be the end of {@link #getTotalPingRange()}, thus not corresponding to a ping.
    *
    * @param instant a time
    * @return the ping index closest to that time
    */
   PingIndex getClosestPingIndex(Instant instant);

   /**
    * {@return the ping for a given ping index}
    *
    * @param pingIndex a ping index
    */
   Ping getPing(PingIndex pingIndex);

   /**
    * Converts a physical depth to echogram depth.
    * <p>
    * Normally, the echogram depth corresponds to physical depth,
    * but this is not the case if the echosounder is bottom mounted.
    *
    * @param physicalDepth a physical depth
    * @return the corresponding echogram depth
    */
   float physicalDepthToDepth(float physicalDepth);
}
