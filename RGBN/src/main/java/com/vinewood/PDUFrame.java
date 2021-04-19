package com.vinewood;

import java.util.Arrays;
import com.vinewood.utils.RGBN_Utils;
import com.vinewood.utils.ChecksumMismatchException;
import com.vinewood.utils.CrcUtil;

/**
 * @author Guo Shuaizhe
 */
public class PDUFrame {
    // frame length 2 bytes
    public byte FrameType;// 1 byte
    public short SeqNo;// 2 bytes
    public short AckNo;// 2 bytes
    public byte[] Data;// DataSize bytes
    public short Checksum;// 2 bytes

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
        byte[] buffer = new byte[9 + dat.length];
        int pos = 0;
        pos = RGBN_Utils.ToByteArray((short) buffer.length, buffer, pos);// length
        buffer[pos++] = ft;// FrameType
        pos = RGBN_Utils.ToByteArray(sn, buffer, pos);// SeqNo
        pos = RGBN_Utils.ToByteArray(an, buffer, pos);// AckNo
        // Data
        for (byte b : dat) {
            buffer[pos++] = b;
        }
        byte[] crc16 = CrcUtil.GetCRC16(dat);
        buffer[pos++] = crc16[0];
        buffer[pos++] = crc16[1];
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
    public static PDUFrame DeserializeFrame(byte[] stream){
        PDUFrame ret = new PDUFrame();
        int pos = 0;
        // pkg length
        int length = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        // FrameType
        ret.FrameType = stream[pos++];
        // SeqNo
        ret.SeqNo = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        // AckNo
        ret.AckNo = RGBN_Utils.ShortFromByteArray(stream, pos);
        pos += 2;
        // Data
        int dataEnd = length - 2;
        // data > 0
        if (pos < dataEnd) {
            ret.Data = Arrays.copyOfRange(stream, pos, dataEnd);
            pos = dataEnd;
        }
        // checksum
        ret.Checksum = RGBN_Utils.ShortFromByteArray(stream, pos);
        return ret;
    }
}
