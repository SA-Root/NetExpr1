package com.vinewood;

import java.util.Arrays;
import com.vinewood.utils.RGBN_Utils;
import com.vinewood.utils.VW_CRC16;
import com.vinewood.utils.ChecksumMismatchException;

/**
 * @author Guo Shuaizhe
 */
public class PDUFrame {
    public byte FrameType;
    public short SeqNo;
    public short AckNo;
    public byte[] Data;
    public short Checksum;
    private static final byte Flag = 0b01110111;

    /**
     * Serialize a PDU frame. Automatically calculates checksum.
     * 
     * @param ft  FrameType
     * @param sn  SeqNo
     * @param an  AckNo
     * @param dat Data
     * @return 0:normal
     */
    public static byte[] SerializeFrame(byte ft, short sn, short an, byte[] dat) {
        byte[] buffer = new byte[9 + dat.length * 2];
        int pos = 0;
        buffer[pos++] = Flag;// LFlag
        buffer[pos++] = ft;// FrameType
        pos = RGBN_Utils.ToByteArray(sn, buffer, pos);// SeqNo
        pos = RGBN_Utils.ToByteArray(an, buffer, pos);// AckNo
        // Data
        for (byte b : dat) {
            if (b == Flag) {
                buffer[pos++] = Flag;
                buffer[pos++] = b;
            } else {
                buffer[pos++] = b;
            }
        }
        pos = RGBN_Utils.ToByteArray((short) VW_CRC16.CRC16CCITT(dat), buffer, pos);// Checksum
        buffer[pos++] = Flag;// RFlag
        byte[] ret = Arrays.copyOfRange(buffer, 0, pos);
        return ret;
    }

    /**
     * Deserialize a PDU frame. Automatically validate checksum.
     * 
     * @param stream Frame stream
     * @return Deserialized frame
     * @throws ChecksumMismatchException
     */
    public static PDUFrame DeserializeFrame(byte[] stream) throws ChecksumMismatchException {
        PDUFrame ret = new PDUFrame();
        int pos = 0;
        // LFlag
        if (stream[pos] != Flag) {
            throw new ChecksumMismatchException();
        }
        ++pos;
        // FrameType
        ret.FrameType = stream[pos++];
        // SeqNo
        ret.SeqNo = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        // AckNo
        ret.AckNo = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        // Data
        int dataEnd = stream.length - 4;
        byte[] buffer = new byte[dataEnd - pos + 1];
        int dataPos = 0;
        while (pos <= dataEnd) {
            if (stream[pos] == Flag) {
                ++pos;
            }
            buffer[dataPos++] = stream[pos++];
        }
        ret.Data = Arrays.copyOf(buffer, buffer.length);
        // Checksum
        short ck = (short) VW_CRC16.CRC16CCITT(ret.Data);
        short crc = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        if (ck != crc) {
            throw new ChecksumMismatchException();
        }
        // RFlag
        if (stream[pos] != Flag) {
            throw new ChecksumMismatchException();
        }
        return ret;
    }
}
