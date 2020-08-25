package no.ks.fiks.streams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FiksPipedInputStreamTest {

    @Test
    @DisplayName("Test at vi kan skrive til og lese fra pipe streams")
    void testSkriveLese() throws IOException {
        try (FiksPipedInputStream inputStream = new FiksPipedInputStream();
             OutputStream outputStream = new PipedOutputStream(inputStream)) {
            int bytes = ThreadLocalRandom.current().nextInt(1000, 10000);
            byte[] originalBytes = new byte[bytes];
            ThreadLocalRandom.current().nextBytes(originalBytes);
            new Thread(() -> {
                try {
                    outputStream.write(originalBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        System.out.println("Failed to close piped output stream");
                    }
                }
            }).start();

            assertArrayEquals(originalBytes, readInputStream(inputStream, bytes));
        }
    }

    @Test
    @DisplayName("Test at RuntimeException satt på input stream fra skriveside blir fanget opp av leseside")
    void testSkriveLeseRuntimeException() throws IOException {
        try (FiksPipedInputStream inputStream = new FiksPipedInputStream();
             OutputStream outputStream = new PipedOutputStream(inputStream)) {
            RuntimeException exception = new RuntimeException(UUID.randomUUID().toString());
            int bytes = ThreadLocalRandom.current().nextInt(1000, 10000);
            byte[] originalBytes = new byte[bytes];
            ThreadLocalRandom.current().nextBytes(originalBytes);
            new Thread(() -> {
                try {
                    outputStream.write(originalBytes);
                    inputStream.setException(exception);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        System.out.println("Failed to close piped output stream");
                    }
                }
            }).start();
            RuntimeException thrown = assertThrows(FiksPipedInputStreamException.class, () -> readInputStream(inputStream, bytes));
            assertThat(thrown.getMessage(), is("Exception satt på PipedInputStream"));
            assertThat(thrown.getCause(), sameInstance(exception));
        }
    }

    @Test
    @DisplayName("Test at IOException satt på input stream fra skriveside blir fanget opp av leseside")
    void testSkriveLeseIOException() throws IOException {
        try (FiksPipedInputStream inputStream = new FiksPipedInputStream();
             OutputStream outputStream = new PipedOutputStream(inputStream)) {
            IOException exception = new IOException(UUID.randomUUID().toString());
            int bytes = ThreadLocalRandom.current().nextInt(1000, 10000);
            byte[] originalBytes = new byte[bytes];
            ThreadLocalRandom.current().nextBytes(originalBytes);
            new Thread(() -> {
                try {
                    outputStream.write(originalBytes);
                    inputStream.setException(exception);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        System.out.println("Failed to close piped output stream");
                    }
                }
            }).start();
            RuntimeException thrown = assertThrows(FiksPipedInputStreamException.class, () -> readInputStream(inputStream, bytes));
            assertThat(thrown.getMessage(), is("Exception satt på PipedInputStream"));
            assertThat(thrown.getCause(), sameInstance(exception));
        }
    }

    @Test
    @DisplayName("Test at exception satt på input stream fra skriveside blir fanget opp av leseside også når ingen bytes er skrevet")
    void testSkriveExceptionIngenBytes() throws IOException {
        try (FiksPipedInputStream inputStream = new FiksPipedInputStream();
             OutputStream outputStream = new PipedOutputStream(inputStream)) {
            RuntimeException exception = new RuntimeException(UUID.randomUUID().toString());
            new Thread(() -> {
                try {
                    inputStream.setException(exception);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        System.out.println("Failed to close piped output stream");
                    }
                }
            }).start();
            RuntimeException thrown = assertThrows(FiksPipedInputStreamException.class, () -> readInputStream(inputStream, 0));
            assertThat(thrown.getMessage(), is("Exception satt på PipedInputStream"));
            assertThat(thrown.getCause(), sameInstance(exception));
        }
    }

    @RepeatedTest(value = 10, name = "{displayName} - {currentRepetition}/{totalRepetitions}")
    @DisplayName("Stress test")
    void stressTest() {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        RuntimeException exception = new RuntimeException(UUID.randomUUID().toString());

        int minSize = 10000;
        int maxSize = 100000;
        int iterations = 10000;

        AtomicInteger successTotal = new AtomicInteger(0);
        AtomicInteger successFail = new AtomicInteger(0);
        AtomicInteger successOk = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            futures.add(pool.submit(() -> {
                boolean shouldFail = ThreadLocalRandom.current().nextBoolean();
                int size = ThreadLocalRandom.current().nextInt(minSize, maxSize);
                int failAt = ThreadLocalRandom.current().nextInt(size);
                try (FiksPipedInputStream inputStream = new FiksPipedInputStream();
                     OutputStream outputStream = new PipedOutputStream(inputStream)) {
                    new Thread(() -> {
                        try {
                            byte[] originalBytes = new byte[size];
                            ThreadLocalRandom.current().nextBytes(originalBytes);
                            if (shouldFail) {
                                outputStream.write(originalBytes, 0, failAt);
                                inputStream.setException(exception);
                            } else {
                                outputStream.write(originalBytes);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                System.out.println("Failed to close piped output stream");
                            }
                        }
                    }).start();
                    if (shouldFail) {
                        RuntimeException thrown = assertThrows(FiksPipedInputStreamException.class, () -> readInputStream(inputStream, size));
                        assertThat(thrown.getMessage(), is("Exception satt på PipedInputStream"));
                        assertThat(thrown.getCause(), sameInstance(exception));
                        successFail.incrementAndGet();
                    } else {
                        readInputStream(inputStream, size);
                        successOk.incrementAndGet();
                    }
                    successTotal.incrementAndGet();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        futures.forEach(p -> {
            try {
                p.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Success ok: " + successOk.get());
        System.out.println("Success failures: " + successFail.get());
        System.out.println("Success total: " + successTotal.get());
    }

    private byte[] readInputStream(InputStream inputStream, int expectedLength) throws IOException {
        byte[] readBytes = new byte[expectedLength];
        int read = inputStream.read();
        int i = 0;
        while (read != -1) {
            readBytes[i++] = (byte) read;
            read = inputStream.read();
        }
        return readBytes;
    }
}
