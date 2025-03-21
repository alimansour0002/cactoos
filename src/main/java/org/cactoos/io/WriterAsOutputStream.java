/*
 * SPDX-FileCopyrightText: Copyright (c) 2017-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package org.cactoos.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Writer as {@link OutputStream}.
 *
 * <p>This class is for internal use only. Use {@link OutputStreamTo}
 * instead.</p>
 *
 * <p>There is no thread-safety guarantee.
 *
 * @since 0.13
 */
final class WriterAsOutputStream extends OutputStream {

    /**
     * Incoming data.
     */
    private final ByteBuffer input;

    /**
     * Output ready to be flushed.
     */
    private final CharBuffer output;

    /**
     * The writer.
     */
    private final Writer writer;

    /**
     * The charset.
     */
    private final CharsetDecoder decoder;

    /**
     * Ctor.
     * @param wtr Writer
     */
    WriterAsOutputStream(final Writer wtr) {
        this(wtr, StandardCharsets.UTF_8);
    }

    /**
     * Ctor.
     * @param wtr Writer
     * @param charset Charset
     */
    WriterAsOutputStream(final Writer wtr, final CharSequence charset) {
        this(wtr, charset, 16 << 10);
    }

    /**
     * Ctor.
     * @param wtr Writer
     * @param charset Charset
     */
    WriterAsOutputStream(final Writer wtr, final Charset charset) {
        this(wtr, charset.name());
    }

    /**
     * Ctor.
     * @param wtr Reader
     * @param charset Charset
     * @param size Buffer size
     */
    WriterAsOutputStream(final Writer wtr, final CharSequence charset,
        final int size) {
        this(wtr, Charset.forName(charset.toString()), size);
    }

    /**
     * Ctor.
     * @param wtr Reader
     * @param size Buffer size
     * @since 0.13.3
     */
    WriterAsOutputStream(final Writer wtr, final int size) {
        this(wtr, StandardCharsets.UTF_8, size);
    }

    /**
     * Ctor.
     * @param wtr Reader
     * @param charset Charset
     * @param size Buffer size
     */
    WriterAsOutputStream(final Writer wtr, final Charset charset,
        final int size) {
        this(
            wtr,
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT),
            size
        );
    }

    /**
     * Ctor.
     * @param wtr Reader
     * @param ddr Charset decoder
     * @param size Buffer size
     */
    WriterAsOutputStream(final Writer wtr, final CharsetDecoder ddr,
        final int size) {
        super();
        this.writer = wtr;
        this.decoder = ddr;
        this.input = ByteBuffer.allocate(size);
        this.output = CharBuffer.allocate(size);
    }

    @Override
    public void write(final int data) throws IOException {
        this.write(new byte[] {(byte) data}, 0, 1);
    }

    @Override
    public void write(final byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    @Override
    public void write(final byte[] buffer, final int offset,
        final int length) throws IOException {
        int left = length;
        int start = offset;
        while (left > 0) {
            final int taken = this.next(buffer, start, left);
            start += taken;
            left -= taken;
        }
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

    /**
     * Write a portion from the buffer.
     * @param buffer The buffer
     * @param offset Offset in the buffer
     * @param length Maximum possible amount to take
     * @return How much was taken
     * @throws IOException If fails
     */
    private int next(final byte[] buffer, final int offset,
        final int length) throws IOException {
        final int max = Math.min(length, this.input.remaining());
        this.input.put(buffer, offset, max);
        this.input.flip();
        while (true) {
            final CoderResult result = this.decoder.decode(
                this.input, this.output, false
            );
            if (result.isError()) {
                result.throwException();
            }
            this.writer.write(
                this.output.array(), 0, this.output.position()
            );
            this.writer.flush();
            this.output.rewind();
            if (result.isUnderflow()) {
                break;
            }
        }
        this.input.compact();
        return max;
    }
}
