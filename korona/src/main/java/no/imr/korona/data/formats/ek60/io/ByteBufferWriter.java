package no.imr.korona.data.formats.ek60.io;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface ByteBufferWriter<T> {
   void write(T item, ByteBuffer byteBuffer);
}
