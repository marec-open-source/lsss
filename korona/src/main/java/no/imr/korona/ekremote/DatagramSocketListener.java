package no.imr.korona.ekremote;

import no.imr.korona.ekremote.responses.ResponseFactory;
import no.imr.tools.concurrent.Exec;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.function.Consumer;

final class DatagramSocketListener {
   private DatagramSocketListener() {
   }

   static void start(DatagramSocket datagramSocket, Consumer<byte[]> listener, Consumer<IOException> failureListener) {
      Exec.CACHED_THREAD_POOL.execute(() -> {
         while (!datagramSocket.isClosed()) {
            try {
               byte[] receiveBytes = new byte[ResponseFactory.MAX_BYTE_SIZE];
               DatagramPacket datagramPacket = new DatagramPacket(receiveBytes, receiveBytes.length);
               datagramSocket.receive(datagramPacket);
               listener.accept(receiveBytes);
            } catch (SocketException e) {
               if (datagramSocket.isClosed()) {
                  return;
               }
               failureListener.accept(e);
            } catch (IOException e) {
               failureListener.accept(e);
            }
         }
      });
   }
}
