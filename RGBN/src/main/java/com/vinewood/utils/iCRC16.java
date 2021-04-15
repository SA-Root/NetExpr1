package com.vinewood.utils;

public interface iCRC16 {
    public byte[] GetCRC16(byte[] data);
    public boolean CheckCRC16(byte[] crc16);
}
