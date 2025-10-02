package no.marec.lsss.api.internal;

import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import no.marec.lsss.api.util.FloatRange;
import no.marec.lsss.api.util.LineStripBuilder;
import no.marec.lsss.api.util.observing.ObservableProperty;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 * This is an implementation detail and is not part of the LSSS API.
 */
public interface InternalObjectFactory {
   <T> ObservableProperty<T> observableProperty(T initialValue);

   Consumer<Object> coalescingObserver(LsssAccess lsssAccess, Runnable observer);

   PingRange pingRange(PingIndex begin, PingIndex end);

   FloatRange floatRange(float begin, float end);

   LineStripBuilder newLineStripBuilder(Path2D path, Rectangle2D bounds);
}
