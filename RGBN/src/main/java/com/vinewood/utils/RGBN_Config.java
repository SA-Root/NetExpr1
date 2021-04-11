package com.vinewood.utils;

import com.alibaba.fastjson.annotation.JSONField;

public class RGBN_Config {
    @JSONField(name = "UDPPort")
    public int UDPPort;
    @JSONField(name = "DataSize")
    public int DataSize;
    @JSONField(name = "ErrorRate")
    public int ErrorRate;
    @JSONField(name = "LostRate")
    public int LostRate;
    @JSONField(name = "SWSize")
    public int SWSize;
    @JSONField(name = "InitSeqNo")
    public int InitSeqNo;
    @JSONField(name = "Timeout")
    public int Timeout;

    public RGBN_Config(int port, int datasize, int erate, int lrate, int swsize, int init, int timeout) {
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

    public void setDataSize(int dataSize) {
        DataSize = dataSize;
    }

    public int getErrorRate() {
        return ErrorRate;
    }

    public void setErrorRate(int errorRate) {
        ErrorRate = errorRate;
    }

    public int getLostRate() {
        return LostRate;
    }

    public void setLostRate(int lostRate) {
        LostRate = lostRate;
    }

    public int getSWSize() {
        return SWSize;
    }

    public void setSWSize(int sWSize) {
        SWSize = sWSize;
    }

    public int getInitSeqNo() {
        return InitSeqNo;
    }

    public void setInitSeqNo(int initSeqNo) {
        InitSeqNo = initSeqNo;
    }

    public int getTimeout() {
        return Timeout;
    }

    public void setTimeout(int timeout) {
        Timeout = timeout;
    }
}
