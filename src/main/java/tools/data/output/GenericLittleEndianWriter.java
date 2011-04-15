package tools.data.output;

import java.awt.Point;
import java.nio.charset.Charset;

public class GenericLittleEndianWriter implements LittleEndianWriter {

    // http://java.sun.com/j2se/1.4.2/docs/api/java/nio/charset/Charset.html
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private ByteOutputStream stream;
    // US-ASCII | Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set

    protected GenericLittleEndianWriter() {
        //
    }

    protected void setByteOutputStream(ByteOutputStream stream) {
        this.stream = stream;
    }

    public GenericLittleEndianWriter(ByteOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void writeZeroBytes(int i) {
        for (int x = 0; x < i; x++) {
            stream.writeByte((byte) 0);
        }
    }

    @Override
    public void write(byte[] bytes) {
        for (int x = 0; x < bytes.length; x++) {
            stream.writeByte(bytes[x]);
        }
    }

    @Override
    public void write(byte b) {
        stream.writeByte(b);
    }

    @Override
    public void write(int i) {
        stream.writeByte((byte) i);
    }

    @Override
    public void writeShort(int i) {
        stream.writeByte((byte) (i & 0xFF));
        stream.writeByte((byte) ((i >>> 8) & 0xFF));
    }

    @Override
    public void writeInt(int i) {
        stream.writeByte((byte) (i & 0xFF));
        stream.writeByte((byte) ((i >>> 8) & 0xFF));
        stream.writeByte((byte) ((i >>> 16) & 0xFF));
        stream.writeByte((byte) ((i >>> 24) & 0xFF));
    }

    @Override
    public void writeInt(long i) {
        stream.writeByte((byte) (i & 0xFF));
        stream.writeByte((byte) ((i >>> 8) & 0xFF));
        stream.writeByte((byte) ((i >>> 16) & 0xFF));
        stream.writeByte((byte) ((i >>> 24) & 0xFF));
    }

    @Override
    public void writeAsciiString(String s) {
        write(s.getBytes(ASCII));
    }

    @Override
    public void writeAsciiString(String s, int max) {
        write(s.getBytes(ASCII));
        for (int i = s.length(); i < max; i++) {
            write(0);
        }
    }

    @Override
    public void writeMapleAsciiString(String s) {
        writeShort((short) s.length());
        writeAsciiString(s);
    }

    @Override
    public void writeNullTerminatedAsciiString(String s) {
        writeAsciiString(s);
        write(0);
    }

    @Override
    public void writePos(Point s) {
        writeShort(s.x);
        writeShort(s.y);
    }

    @Override
    public void writeLong(long l) {
        stream.writeByte((byte) (l & 0xFF));
        stream.writeByte((byte) ((l >>> 8) & 0xFF));
        stream.writeByte((byte) ((l >>> 16) & 0xFF));
        stream.writeByte((byte) ((l >>> 24) & 0xFF));
        stream.writeByte((byte) ((l >>> 32) & 0xFF));
        stream.writeByte((byte) ((l >>> 40) & 0xFF));
        stream.writeByte((byte) ((l >>> 48) & 0xFF));
        stream.writeByte((byte) ((l >>> 56) & 0xFF));
    }
}