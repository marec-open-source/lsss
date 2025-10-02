package no.marec.api.korona;

/**
 * Buffered pings as needed by {@link ModuleComputation}.
 */
public interface PingBuffer {
   /**
    * The first valid index usable by {@link #getPing(int)}.
    *
    * @return the first valid index
    */
   int getBeginIndex();

   /**
    * One past the last valid index usable by {@link #getPing(int)}.
    *
    * @return one past the last valid index
    */
   int getEndIndex();

   /**
    * Returns a ping indexed relative to the center ping.
    * The center ping can be accessed by {@code getPing(0)}.
    * The surrounding pings can be access by {@code getPing(index)}, where
    * <blockquote>
    * {@link #getBeginIndex()} &le; index &lt; {@link #getEndIndex()}.
    * </blockquote>
    *
    * @param index the index relative to the center ping
    * @return a ping
    */
   Ping getPing(int index);
}
