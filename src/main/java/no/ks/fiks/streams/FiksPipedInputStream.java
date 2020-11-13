package no.ks.fiks.streams;

import java.io.IOException;
import java.io.PipedInputStream;

public class FiksPipedInputStream extends PipedInputStream {

    private volatile Exception exception = null;

    @Override
    public synchronized int read() throws IOException {
        int read = super.read();
        checkException();
        return read;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        checkException();
        return read;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    private void checkException() {
        if (exception != null) {
            throw new FiksPipedInputStreamException("Exception satt på PipedInputStream", exception);
        }
    }
}
