package no.marec.api.korona;

/**
 * KORONA module computations that use more than just a single ping.
 * <p>
 * Only the center ping should be modified.
 * The buffered surrounding pings, specified by {@link #getPingBufferRadius()}, are only for reading.
 * <p>
 * Computation that do not require buffering could use {@link SinglePingModuleComputation}.
 */
public interface ModuleComputation {
   /**
    * The (maximum) number of pings that should be made available for reading on either side of the center ping.
    * The total number of pings accessible will be up to {@code 2 × PingBufferRadius + 1}.
    * At the beginning and end of the file fewer pings will be available, as specified by
    * the functions {@link PingBuffer#getBeginIndex()} and {@link PingBuffer#getEndIndex()}.
    *
    * @return number of read-only pings on either side of the center ping
    */
   int getPingBufferRadius();

   /**
    * The computation implementation.
    * <p>
    * Note: Only the center ping, with index 0, should be modified.
    *
    * @param pingBuffer the buffer containing the accessible ping
    */
   void compute(PingBuffer pingBuffer);
}
