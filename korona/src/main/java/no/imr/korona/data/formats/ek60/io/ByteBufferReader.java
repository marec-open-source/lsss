package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramFormatException;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface ByteBufferReader<T> {
   T read(ByteBuffer byteBuffer) throws DatagramFormatException;
}
