package com.smp.smptools.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that limits the number of bytes read.
 * Prevents memory exhaustion from large or endless responses.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public class BoundedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long bytesRead = 0;

    /**
     * Creates a BoundedInputStream wrapping the given input stream.
     *
     * @param in the input stream to wrap
     * @param maxBytes the maximum number of bytes allowed (use -1 for unlimited)
     */
    public BoundedInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        if (maxBytes >= 0 && bytesRead >= maxBytes) {
            throw new IOException("Download exceeded maximum size of " + maxBytes + " bytes");
        }
        int b = super.read();
        if (b >= 0) {
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (maxBytes >= 0 && bytesRead >= maxBytes) {
            throw new IOException("Download exceeded maximum size of " + maxBytes + " bytes");
        }
        long remaining = maxBytes - bytesRead;
        if (maxBytes >= 0 && len > remaining) {
            len = (int) remaining;
        }
        int read = super.read(b, off, len);
        if (read > 0) {
            bytesRead += read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (maxBytes >= 0 && bytesRead >= maxBytes) {
            throw new IOException("Download exceeded maximum size of " + maxBytes + " bytes");
        }
        long remaining = maxBytes - bytesRead;
        if (maxBytes >= 0 && n > remaining) {
            n = remaining;
        }
        long skipped = super.skip(n);
        bytesRead += skipped;
        return skipped;
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
