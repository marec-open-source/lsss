package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import no.marec.lsss.api.internal.InternalObjectFactory;
import no.marec.lsss.api.util.FloatRange;
import no.marec.lsss.api.util.LineStripBuilder;
import no.marec.lsss.api.util.observing.ObservableProperty;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

final class InternalObjectFactoryImpl implements InternalObjectFactory {
   static final InternalObjectFactoryImpl INSTANCE = new InternalObjectFactoryImpl();

   private InternalObjectFactoryImpl() {
   }

   @Override
   public <T> ObservableProperty<T> observableProperty(T initialValue) {
      return new ListenableProperty<>(initialValue);
   }

   @Override
   public Consumer<Object> coalescingObserver(LsssAccess lsssAccess, Runnable observer) {
      LSSS lsss = ((LsssAccessImpl) lsssAccess).getLsss();
      return lsss.getInterpretationSettings().createCoalescingListener(observer);
   }

   @Override
   public PingRange pingRange(PingIndex begin, PingIndex end) {
      if (begin instanceof no.imr.korona.data.ping.PingIndex b
            && end instanceof no.imr.korona.data.ping.PingIndex e) {
         return no.imr.korona.data.ping.PingRange.of(b, e);
      }
      throw new IllegalArgumentException();
   }

   @Override
   public FloatRange floatRange(float begin, float end) {
      return no.imr.tools.range.FloatRange.of(begin, end);
   }

   @Override
   public LineStripBuilder newLineStripBuilder(Path2D path, Rectangle2D bounds) {
      return LineStripBuilders.coalescing(path, bounds);
   }
}
