package no.marec.lsss.api.util;

import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import no.marec.lsss.api.internal.InternalLsss;
import no.marec.lsss.api.internal.InternalObjectFactory;
import no.marec.lsss.api.util.observing.ObservableProperty;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 * For creating new instances of objects represented in the LSSS API by interfaces.
 */
public final class ObjectFactory {
   private static final InternalObjectFactory FACTORY = InternalLsss.INSTANCE.objectFactory();

   private ObjectFactory() {
   }

   /**
    * {@return a new observable property}
    *
    * @param initialValue the initial value
    * @param <T>          the value type
    */
   public static <T> ObservableProperty<T> observableProperty(T initialValue) {
      return FACTORY.observableProperty(initialValue);
   }

   /**
    * {@return a coalescing observer}
    * <p>
    * Notifications to the underlying observer will happen sequentially at some time
    * after the returned coalescing observer has been notified one or more times.
    *
    * @param lsssAccess an LSSS instance
    * @param observer   an observer
    */
   public static Consumer<Object> coalescingObserver(LsssAccess lsssAccess, Runnable observer) {
      return FACTORY.coalescingObserver(lsssAccess, observer);
   }

   /**
    * {@return a new ping range}
    *
    * @param begin the start value
    * @param end   the end value
    */
   public static PingRange pingRange(PingIndex begin, PingIndex end) {
      return FACTORY.pingRange(begin, end);
   }

   /**
    * {@return a new float range}
    *
    * @param begin the start value
    * @param end   the end value
    */
   public static FloatRange floatRange(float begin, float end) {
      return FACTORY.floatRange(begin, end);
   }

   /**
    * {@return a new line strip builder}
    * <p>
    * The resulting line strips are optimized such that
    * subsequent line segments with the same slope are coalesced,
    * and line segments outside the {@code bounds} are discarded.
    *
    * @param path   the path to add points to
    * @param bounds bounds for the resulting line strips
    */
   public static LineStripBuilder lineStripBuilder(Path2D path, Rectangle2D bounds) {
      return FACTORY.newLineStripBuilder(path, bounds);
   }
}
