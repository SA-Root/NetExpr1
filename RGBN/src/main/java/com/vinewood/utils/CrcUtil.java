package com.vinewood.utils;

public class CrcUtil {

    /**
     * 为Byte数组最后添加两位CRC校验
     *
     * @param Data（验证的byte数组）
     * @return 添加了CRC CCITT验证的数组
     */
    public static byte[] SetCRC(byte[] Data) {
        int checkCode = 0;
        checkCode = crc_16_CCITT_False(Data);
        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((checkCode >> 8) & 0xff);
        crcByte[1] = (byte) (checkCode & 0xff);
        // 将新生成的byte数组添加到原数据结尾并返回
        return concatAll(Data, crcByte);
    }

    /**
     * CRC-16/CCITT-FALSE x16+x12+x5+1 算法
     * 计算CRC16CCITT
     * info
     * Name:CRC-16/CCITT-FAI
     * Width:16
     * Poly:0x1021
     * Init:0xFFFF
     * RefIn:False
     * RefOut:False
     * XorOut:0x0000
     *
     * @param bytes
     * @return
     */
    public static int crc_16_CCITT_False(byte[] bytes) {
        int crc = 0xffff; // initial value
        int polynomial = 0x1021; // poly value
        for (int index = 0; index < bytes.length; index++) {
            byte b = bytes[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        //输出String字样的16进制
        String strCrc = Integer.toHexString(crc).toUpperCase();
        System.out.println(strCrc);
        return crc;
    }

    /***
     * CRC校验是否通过
     *
     * @param srcByte
     * @param length(验证码字节长度)
     * @return
     */
    public static boolean CheckCRC(byte[] srcByte, int length) {

        // 取出除crc校验位的其他数组，进行计算，得到CRC校验结果
        int calcCRC = calcCRC(srcByte, 0, srcByte.length - length);
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((calcCRC >> 8) & 0xff);
        bytes[1] = (byte) (calcCRC & 0xff);

        // 取出CRC校验位，进行计算
        int i = srcByte.length;
        byte[] b = { srcByte[i - 2] ,srcByte[i - 1] };

        // 比较
        return bytes[0] == b[0] && bytes[1] == b[1];
    }

    /**
     * 对buf中offset以前crcLen长度的字节作crc校验，返回校验结果
     * @param  buf
     * @param crcLen
     */
    private static int calcCRC(byte[] buf, int offset, int crcLen) {
        int start = offset;
        int end = offset + crcLen;
        int crc = 0xffff; // initial value
        int polynomial = 0x1021;
        for (int index = start; index < end; index++) {
            byte b = buf[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        return crc;
    }

    /**
     * 多个数组合并
     *
     * @param first
     * @param rest
     * @return
     */
    public static byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
