package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.Utils;
import no.imr.tools.math.ComplexArray;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.StringTokenizer;

/**
 * The EK80 echosounder filter configuration datagram.
 */
public final class Fil0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("FIL0", Fil0Datagram::new);

   private final short channel;     // Channel, int16
   private final String channelId;   // Channel ID, char 128
   private final double g;           // Part of filter
   private final ComplexArray filter;
   private final byte[] bytes; // todo: remove when write reproduces read

   public Fil0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      bytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(bytes);
      byteBuffer.position(byteBuffer.position() - bytes.length);

      StringTokenizer tokenizer;

      channel = byteBuffer.getShort();
      channelId = ByteBufferUtils.readCString(byteBuffer, 128);   // e.g. "WBT 3-1 10.1.48.54 ES70-7C"

      try {
         // Hac to solve problems with leading zeros
         while (byteBuffer.hasRemaining() && byteBuffer.get(byteBuffer.position()) == 0) {
            byteBuffer.get();
         }

         String filterData = ByteBufferUtils.readCString(byteBuffer, byteBuffer.remaining());
         filterData = filterData.replace('\n', ' ');
         tokenizer = new StringTokenizer(filterData, " ");
         if (!tokenizer.hasMoreTokens()) {
            throw new DatagramFormatException("Fil0 datagram: not more tokens");
         }
         g = Double.parseDouble(tokenizer.nextToken());
      } catch (NumberFormatException e) {
         throw new DatagramFormatException(e);
      }

      try {  // Get filter coefficients from hex string: real as MSB, imaginary as LSB
         filter = ComplexArray.ofLength(tokenizer.countTokens());
         int i = 0;
         while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken(); //Hex string with lowercase letters (%x), e.g. "0x7fff"
            s = s.replace("0x", "");          //Remove "0x" to get hexagonal - hopefully OK
            long n = Long.parseLong(s, 16);  //Need to be "long": "int" will sometimes be too small to keep the number
            double re = (short) (n & 0xffff);          //e.g. re = {32767, 31267, 27075, 21035, 14290,  7985,  2993,  -252, ...
            double im = (short) (n >> 16 & 0xffff);    //e.g. im = {    0,  7247, 13264, 17125, 18422, 17320, 14452, 10698, ...
            filter.set(i, re, im);
            i++;
         }
      } catch (NumberFormatException e) {
         throw new DatagramFormatException(e);
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) { // todo: RK check: not sure about this. Certainly not finished yet!
      if (bytes.length > 0) {
         byteBuffer.put(bytes);
         return;
      }

      byteBuffer.putShort(channel);
      ByteBufferUtils.writeCString(byteBuffer, channelId, 128);
      ByteBufferUtils.writeCString(byteBuffer, Double.toString(g));
      ByteBufferUtils.writeCString(byteBuffer, "\n");
      for (int i = 0; i < filter.length(); i++) {
         long re = 0xffff & Math.round(filter.re(i));
         long im = 0xffff & Math.round(filter.im(i));
         String str = Utils.format("0x%04x%04x\n", im, re);
         ByteBufferUtils.writeCString(byteBuffer, str);
      }
   }

   @Override
   public String toStringExtra() {
      return "channel: " + channel
            + ", " + channelId
            + ", g: " + g
            + ", filterCount: " + filter.length()
            + ", filter: " + filter.toComplexValuesString();
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public short getChannel() {
      return channel;
   }

   public String getChannelId() {
      return channelId;
   }

   public double getG() {
      return g;
   }

   public ComplexArray getFilter() {
      return filter;
   }
}
