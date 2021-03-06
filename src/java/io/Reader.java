/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.io;

import java.nio.CharBuffer;
import java.util.Objects;

/**
 * Abstract class for reading character streams.  The only methods that a
 * subclass must implement are read(char[], int, int) and close().  Most
 * subclasses, however, will override some of the methods defined here in order
 * to provide higher efficiency, additional functionality, or both.
 *
 * @author Mark Reinhold
 * @see BufferedReader
 * @see LineNumberReader
 * @see CharArrayReader
 * @see InputStreamReader
 * @see FileReader
 * @see FilterReader
 * @see PushbackReader
 * @see PipedReader
 * @see StringReader
 * @see Writer
 * @since 1.1
 */
/*
 * 字符输入流，可以从中读取数据
 *
 * 本地 <--------- 远端
 *
 * 字符输入流的解释：
 * [输入流]的含义是从远端读取数据到本地，
 * [字符]的含义是本地获取到的是字符(远端通常是数组、字节流、字符流、通道)
 *
 * 注：这里的字符指char
 */
public abstract class Reader implements Readable, Closeable {
    
    private static final int TRANSFER_BUFFER_SIZE = 8192;
    
    /** Maximum skip-buffer size */
    private static final int maxSkipBufferSize = 8192;
    
    /**
     * The object used to synchronize operations on this stream.  For
     * efficiency, a character-stream object may use an object other than
     * itself to protect critical sections.  A subclass should therefore use
     * the object in this field rather than {@code this} or a synchronized
     * method.
     */
    protected Object lock;
    
    /** Skip buffer, null until allocated */
    private char[] skipBuffer = null;   // 临时存储跳过的字符
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new character-stream reader whose critical sections will synchronize on the reader itself.
     */
    protected Reader() {
        this.lock = this;
    }
    
    /**
     * Creates a new character-stream reader whose critical sections will synchronize on the given object.
     *
     * @param lock The Object to synchronize on.
     */
    protected Reader(Object lock) {
        if(lock == null) {
            throw new NullPointerException();
        }
        
        this.lock = lock;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single character.  This method will block until a character is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p> Subclasses that intend to support efficient single-character input
     * should override this method.
     *
     * @return The character read, as an integer in the range 0 to 65535
     * ({@code 0x00-0xffff}), or -1 if the end of the stream has
     * been reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 返回从字符输入流中读取的一个char，返回-1表示读取失败
    public int read() throws IOException {
        char[] cb = new char[1];
        
        if(read(cb, 0, 1) == -1) {
            return -1;
        } else {
            return cb[0];
        }
    }
    
    /**
     * Reads characters into an array.  This method will block until some input
     * is available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param cbuf Destination buffer
     *
     * @return The number of characters read, or -1
     * if the end of the stream
     * has been reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 尝试从字符输入流中读取足量的char填满cbuf。返回实际填充的字符数量，返回-1表示读取失败
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }
    
    /**
     * Reads characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len  Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the
     * stream has been reached
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException If {@code off} is negative, or {@code len} is negative,
     *                                   or {@code len} is greater than {@code cbuf.length - off}
     */
    // 尝试从字符输入流中读取len个char，并将其填充到cbuf的off处。返回实际填充的字符数量，返回-1表示读取失败
    public abstract int read(char[] cbuf, int off, int len) throws IOException;
    
    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed.
     *
     * @param target the buffer to read characters into
     *
     * @return The number of characters added to the buffer, or
     * -1 if this source of characters is at its end
     *
     * @throws IOException                      if an I/O error occurs
     * @throws NullPointerException             if target is null
     * @throws java.nio.ReadOnlyBufferException if target is a read only buffer
     * @since 1.5
     */
    // 尝试从字符输入流中读取足量的char填满target。返回实际填充的字符数量，返回-1表示读取失败
    public int read(CharBuffer target) throws IOException {
        // 获取缓冲区中还剩多少空间
        int len = target.remaining();
        // 创建等大的字符数组
        char[] cbuf = new char[len];
        
        // 从字符输入流中读取足量的字符填满字符数组，返回实际读到的字符数量
        int n = read(cbuf, 0, len);
        
        if(n>0) {
            // 将源字符数组cbuf中的所有字符写入到target缓冲区
            target.put(cbuf, 0, n);
        }
        
        return n;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether this stream supports the mark() operation.
     * The default implementation always returns false.
     * Subclasses should override this method.
     *
     * @return true if and only if this stream supports the mark operation.
     */
    // 判断当前输入流是否支持存档标记，默认不支持
    public boolean markSupported() {
        return false;
    }
    
    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.  Not all
     * character-input streams support the mark() operation.
     *
     * @param readAheadLimit Limit on the number of characters that may be
     *                       read while still preserving the mark.  After
     *                       reading this many characters, attempting to
     *                       reset the stream may fail.
     *
     * @throws IOException If the stream does not support mark(),
     *                     or if some other I/O error occurs
     */
    // 设置存档标记，readAheadLimit是存档区预读上限
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }
    
    /**
     * Resets the stream.
     * If the stream has been marked, then attempt to reposition it at the mark.
     * If the stream has not been marked, then attempt to reset it in some way appropriate to the particular stream,
     * for example by repositioning it to its starting point.
     * Not all character-input streams support the reset() operation,
     * and some support reset() without supporting mark().
     *
     * @throws IOException If the stream has not been marked,
     *                     or if the mark has been invalidated,
     *                     or if the stream does not support reset(),
     *                     or if some other I/O error occurs
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置
    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转移 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads all characters from this reader and writes the characters to the
     * given writer in the order that they are read. On return, this reader
     * will be at end of the stream. This method does not close either reader
     * or writer.
     * <p>
     * This method may block indefinitely reading from the reader, or
     * writing to the writer. The behavior for the case where the reader
     * and/or writer is <i>asynchronously closed</i>, or the thread
     * interrupted during the transfer, is highly reader and writer
     * specific, and therefore not specified.
     * <p>
     * If an I/O error occurs reading from the reader or writing to the
     * writer, then it may do so after some characters have been read or
     * written. Consequently the reader may not be at end of the stream and
     * one, or both, streams may be in an inconsistent state. It is strongly
     * recommended that both streams be promptly closed if an I/O error occurs.
     *
     * @param out the writer, non-null
     *
     * @return the number of characters transferred
     *
     * @throws IOException          if an I/O error occurs when reading or writing
     * @throws NullPointerException if {@code out} is {@code null}
     * @since 10
     */
    // 将当前输入流中的字节转移到输出流中，返回值表示成功转移的字节数
    public long transferTo(Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        
        long transferred = 0;
        char[] buffer = new char[TRANSFER_BUFFER_SIZE];
        int nRead;
        
        while((nRead = read(buffer, 0, TRANSFER_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, nRead);
            transferred += nRead;
        }
        
        return transferred;
    }
    
    /*▲ 转移 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the stream and releases any system resources associated with
     * it.  Once the stream has been closed, further read(), ready(),
     * mark(), reset(), or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @throws IOException If an I/O error occurs
     */
    // 关闭输入流
    public abstract void close() throws IOException;
    
    /**
     * Tells whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise.  Note that returning false does not guarantee that the
     * next read will block.
     *
     * @throws IOException If an I/O error occurs
     */
    // 判断当前流是否已准备好被读取
    public boolean ready() throws IOException {
        return false;
    }
    
    /**
     * Skips characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param n The number of characters to skip
     *
     * @return The number of characters actually skipped
     *
     * @throws IllegalArgumentException If <code>n</code> is negative.
     * @throws IOException              If an I/O error occurs
     */
    // 读取中跳过n个字符，返回实际跳过的字符数
    public long skip(long n) throws IOException {
        if(n<0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        
        int nn = (int) Math.min(n, maxSkipBufferSize);
        
        synchronized(lock) {
            if((skipBuffer == null) || (skipBuffer.length<nn)) {
                skipBuffer = new char[nn];
            }
            
            long r = n;
            while(r>0) {
                int nc = read(skipBuffer, 0, (int) Math.min(r, nn));
                if(nc == -1) {
                    break;
                }
                
                r -= nc;
            }
            
            return n - r;
        }
    }
    
    /**
     * Returns a new {@code Reader} that reads no characters. The returned
     * stream is initially open.  The stream is closed by calling the
     * {@code close()} method.  Subsequent calls to {@code close()} have no
     * effect.
     *
     * <p> While the stream is open, the {@code read()}, {@code read(char[])},
     * {@code read(char[], int, int)}, {@code read(Charbuffer)}, {@code
     * ready()}, {@code skip(long)}, and {@code transferTo()} methods all
     * behave as if end of stream has been reached. After the stream has been
     * closed, these methods all throw {@code IOException}.
     *
     * <p> The {@code markSupported()} method returns {@code false}.  The
     * {@code mark()} and {@code reset()} methods throw an {@code IOException}.
     *
     * <p> The {@link #lock object} used to synchronize operations on the
     * returned {@code Reader} is not specified.
     *
     * @return a {@code Reader} which reads no characters
     *
     * @since 11
     */
    // 返回一个不包含有效字符的输入流
    public static Reader nullReader() {
        return new Reader() {
            private volatile boolean closed;
            
            @Override
            public int read() throws IOException {
                ensureOpen();
                return -1;
            }
            
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, cbuf.length);
                ensureOpen();
                if(len == 0) {
                    return 0;
                }
                return -1;
            }
            
            @Override
            public int read(CharBuffer target) throws IOException {
                Objects.requireNonNull(target);
                ensureOpen();
                if(target.hasRemaining()) {
                    return -1;
                }
                return 0;
            }
            
            @Override
            public boolean ready() throws IOException {
                ensureOpen();
                return false;
            }
            
            @Override
            public long skip(long n) throws IOException {
                ensureOpen();
                return 0L;
            }
            
            @Override
            public long transferTo(Writer out) throws IOException {
                Objects.requireNonNull(out);
                ensureOpen();
                return 0L;
            }
            
            @Override
            public void close() {
                closed = true;
            }
            
            private void ensureOpen() throws IOException {
                if(closed) {
                    throw new IOException("Stream closed");
                }
            }
        };
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
