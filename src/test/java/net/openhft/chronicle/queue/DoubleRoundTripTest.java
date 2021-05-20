package net.openhft.chronicle.queue;

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DoubleRoundTripTest {

    private final SingleChronicleQueue queue;

    public DoubleRoundTripTest(WireType wireType) throws IOException {
        this.queue = SingleChronicleQueueBuilder.binary(Files.createTempDirectory("DoubleRoundTripTest"))
                .wireType(wireType)
                .build();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.COMPRESSED_BINARY},
                new Object[]{WireType.FIELDLESS_BINARY}
        );
    }

    @Test
    public void canWriteNaturalDoubleGreaterThanIntegerMax() throws IOException {
        final ExcerptAppender appender = queue.acquireAppender();
        try (final DocumentContext writeDc = appender.writingDocument();
             final ObjectOutput objectOutput = writeDc.wire().objectOutput()) {
            objectOutput.writeDouble(2000000000.0);
            objectOutput.writeDouble(3000000000.0);
            objectOutput.writeDouble(3000000000.1);
        }

        final ExcerptTailer tailer = queue.createTailer();
        try (final DocumentContext readDc = tailer.readingDocument();
             final ObjectInput objectInput = readDc.wire().objectInput()) {
            assertEquals(2000000000.0, objectInput.readDouble(), 0.00001);
            assertEquals(3000000000.0, objectInput.readDouble(), 0.00001);
            assertEquals(3000000000.1, objectInput.readDouble(), 0.00001);
        }
    }
}
