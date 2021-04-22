package com.vinewood.utils;

/**
 * @author Guo Shuaizhe
 */
public class RGBN_Utils {
    public enum PDUErrorLoss {
        ERROR, LOSS, NORMAL
    }

    /**
     * Big-endian
     * 
     * @param n      Number
     * @param target Target Array
     * @param offset Offset
     * @return New Offset
     */
    public static int ToByteArray(short n, byte[] target, int offset) {
        target[offset++] = (byte) (n >> 8);
        target[offset++] = (byte) (n);
        return offset;
    }

    /**
     * Big-endian
     * 
     * @param stream Source array
     * @param offset Offset
     * @return Short number
     */
    public static short ShortFromByteArray(byte[] stream, int offset) {
        return (short) ((stream[offset] & 0xFF) << 8 | (stream[offset + 1] & 0xFF));
    }

    public static int IntFromByteArray(byte[] stream, int offset) {
        return ((stream[offset] & 0xFF) << 24 | (stream[offset + 1] & 0xFF) << 16 | (stream[offset + 2] & 0xFF) << 8
                | (stream[offset + 3] & 0xFF));
    }
    
    /**
     * Generate random loss & error PDU
     * 
     * @param err  1 Error PDU per [err] PDUs
     * @param loss 1 Loss PDU per [loss] PDUs
     * @return Random result
     */
    public static PDUErrorLoss ErrorLossGenerator(int err, int loss) {
        double RateE = 1.0 / err, RateL = 1.0 / loss;
        double res = Math.random();
        if (res <= RateE) {
            return PDUErrorLoss.ERROR;
        } else {
            if (res <= RateE + RateL) {
                return PDUErrorLoss.LOSS;
            } else {
                return PDUErrorLoss.NORMAL;
            }
        }
    }
}
// ByteBuffer.allocate(4).putInt(n).array();