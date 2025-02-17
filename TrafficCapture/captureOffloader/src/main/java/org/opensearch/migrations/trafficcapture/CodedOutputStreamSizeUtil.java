package org.opensearch.migrations.trafficcapture;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Timestamp;
import org.opensearch.migrations.trafficcapture.protos.EndOfSegmentsIndication;
import org.opensearch.migrations.trafficcapture.protos.TrafficObservation;
import org.opensearch.migrations.trafficcapture.protos.TrafficStream;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Utility functions for computing sizes of fields to be added to a CodedOutputStream
 */
public class CodedOutputStreamSizeUtil {

    public static int getSizeOfTimestamp(Instant t) {
        long seconds = t.getEpochSecond();
        int nanos = t.getNano();
        var secSize = CodedOutputStream.computeInt64Size(Timestamp.SECONDS_FIELD_NUMBER, seconds);
        var nanoSize = nanos == 0 ? 0 : CodedOutputStream.computeInt32Size(Timestamp.NANOS_FIELD_NUMBER, nanos);
        return secSize + nanoSize;
    }

    /**
     * This function calculates the maximum bytes that would be needed to store a [Read/Write]SegmentObservation, if constructed
     * from the given ByteBuffer and associated segment field numbers and values passed in. This estimate is essentially
     * the max size needed in the CodedOutputStream to store the provided ByteBuffer data and its associated TrafficStream
     * overhead. The actual required bytes could be marginally smaller.
     */
    public static int maxBytesNeededForASegmentedObservation(Instant timestamp, int observationFieldNumber, int dataFieldNumber,
        int dataCountFieldNumber, int dataCount,  ByteBuffer buffer, int numberOfTrafficStreamsSoFar) {
        // Timestamp required bytes
        int tsContentSize = getSizeOfTimestamp(timestamp);
        int tsTagAndContentSize = CodedOutputStream.computeInt32Size(TrafficObservation.TS_FIELD_NUMBER, tsContentSize) + tsContentSize;

        // Capture required bytes
        int dataSize = CodedOutputStream.computeByteBufferSize(dataFieldNumber, buffer);
        int dataCountSize = dataCountFieldNumber > 0 ? CodedOutputStream.computeInt32Size(dataCountFieldNumber, dataCount) : 0;
        int captureContentSize = dataSize + dataCountSize;
        int captureTagAndContentSize = CodedOutputStream.computeInt32Size(observationFieldNumber, captureContentSize) + captureContentSize;

        // Observation and closing index required bytes
        return bytesNeededForObservationAndClosingIndex(tsTagAndContentSize + captureTagAndContentSize, numberOfTrafficStreamsSoFar);
    }

    /**
     * This function determines the number of bytes needed to store a TrafficObservation and a closing index for a
     * TrafficStream, from the provided input.
     */
    public static int bytesNeededForObservationAndClosingIndex(int observationContentSize, int numberOfTrafficStreamsSoFar) {
        int observationTagSize = CodedOutputStream.computeUInt32Size(TrafficStream.SUBSTREAM_FIELD_NUMBER, observationContentSize);

        // Size for TrafficStream index added when flushing, use arbitrary field to calculate
        int indexSize = CodedOutputStream.computeInt32Size(TrafficStream.NUMBEROFTHISLASTCHUNK_FIELD_NUMBER, numberOfTrafficStreamsSoFar);

        return observationTagSize + observationContentSize + indexSize;
    }


}
