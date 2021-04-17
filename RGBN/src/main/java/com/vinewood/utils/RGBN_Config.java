package com.vinewood.utils;

import com.alibaba.fastjson.annotation.JSONField;

public class RGBN_Config {
    @JSONField(name = "UDPPort")
    public int UDPPort;
    @JSONField(name = "DataSize")
    public short DataSize;
    @JSONField(name = "ErrorRate")
    public short ErrorRate;
    @JSONField(name = "LostRate")
    public short LostRate;
    @JSONField(name = "SWSize")
    public short SWSize;
    @JSONField(name = "InitSeqNo")
    public short InitSeqNo;
    @JSONField(name = "Timeout")
    public short Timeout;
    public RGBN_Config()
    {
    }
    public RGBN_Config(int port, short datasize, short erate, short lrate, short swsize, short init, short timeout) {
        UDPPort = port;
        DataSize = datasize;
        ErrorRate = erate;
        LostRate = lrate;
        SWSize = swsize;
        InitSeqNo = init;
        Timeout = timeout;
    }

    public int getUDPPort() {
        return UDPPort;
    }

    public void setUDPPort(int uDPPort) {
        UDPPort = uDPPort;
    }

    public int getDataSize() {
        return DataSize;
    }

    public void setDataSize(short dataSize) {
        DataSize = dataSize;
    }

    public int getErrorRate() {
        return ErrorRate;
    }

    public void setErrorRate(short errorRate) {
        ErrorRate = errorRate;
    }

    public int getLostRate() {
        return LostRate;
    }

    public void setLostRate(short lostRate) {
        LostRate = lostRate;
    }

    public int getSWSize() {
        return SWSize;
    }

    public void setSWSize(short sWSize) {
        SWSize = sWSize;
    }

    public int getInitSeqNo() {
        return InitSeqNo;
    }

    public void setInitSeqNo(short initSeqNo) {
        InitSeqNo = initSeqNo;
    }

    public int getTimeout() {
        return Timeout;
    }

    public void setTimeout(short timeout) {
        Timeout = timeout;
    }
}
