package no.marec.api.korona;

/**
 * KORONA module computations that use only a single ping.
 */
public abstract class SinglePingModuleComputation implements ModuleComputation {
   /**
    * Constructor for subclasses.
    */
   protected SinglePingModuleComputation() {
   }

   /**
    * No buffering.
    *
    * @return 0
    */
   @Override
   public final int getPingBufferRadius() {
      return 0;
   }

   /**
    * Calls {@link #compute(Ping)} on the single ping in the ping buffer.
    *
    * @param pingBuffer the ping buffer
    */
   @Override
   public final void compute(PingBuffer pingBuffer) {
      compute(pingBuffer.getPing(0));
   }

   /**
    * Do computations on a single ping.
    *
    * @param ping the ping to do computations on
    */
   public abstract void compute(Ping ping);
}
