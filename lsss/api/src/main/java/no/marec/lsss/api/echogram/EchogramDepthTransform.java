package no.marec.lsss.api.echogram;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.PingIndex;

import java.time.Instant;

/**
 * Converts between depth and echogram image y-coordinate.
 */
@DoNotImplement
public interface EchogramDepthTransform {
   /**
    * {@return the y-coordinate corresponding to a depth}
    *
    * @param depth   a depth
    * @param instant a time
    */
   double depthToY(double depth, Instant instant);

   /**
    * {@return the y-coordinate corresponding to a depth}
    *
    * @param depth     a depth
    * @param pingIndex a ping index
    */
   double depthToY(double depth, PingIndex pingIndex);

   /**
    * {@return the depth corresponding to a y-coordinate}
    *
    * @param y       a y-coordinate
    * @param instant a time
    */
   double yToDepth(double y, Instant instant);

   /**
    * {@return the depth corresponding to a y-coordinate}
    *
    * @param y         a y-coordinate
    * @param pingIndex a ping index
    */
   double yToDepth(double y, PingIndex pingIndex);
}
