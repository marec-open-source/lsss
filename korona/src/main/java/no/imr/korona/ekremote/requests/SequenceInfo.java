package no.imr.korona.ekremote.requests;

/**
 * Sequence no, Current msg no, Total msg no [22].
 */
public record SequenceInfo(int sequenceNumber, int currentMessageNumber, int totalMessageNumber) {
}
