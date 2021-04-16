package com.vinewood;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import com.vinewood.utils.RGBN_Config;

public class Sender implements iUDPInstance {
    private String IPAddress;
    private String FilePath;
    //For receiving
    private int AckExpected;
    //For sending
    private int AckReceived;
    //next index of DataSegments
    private int NextToSend;
    private List<byte[]> DataSegments;
    private RGBN_Config cfg;
    private int PacketSize;

    public Sender(String ip_addr, String fpath, RGBN_Config config) {
        IPAddress = ip_addr;
        FilePath = fpath;
        cfg = config;
        AckReceived = cfg.InitSeqNo;
        AckExpected = cfg.InitSeqNo + 1;
        NextToSend = cfg.InitSeqNo;
        PacketSize = cfg.DataSize + 9;
        ReadFile();
    }

    public void ReadFile() {
        File f = new File(FilePath);
        try {
            FileInputStream fStream = new FileInputStream(f);
            byte[] Data = fStream.readAllBytes();
            fStream.close();
            int LastToSend;
            if (Data.length % cfg.DataSize == 0) {
                LastToSend = Data.length / cfg.DataSize + cfg.InitSeqNo - 1;
            } else {
                LastToSend = Data.length / cfg.DataSize + cfg.InitSeqNo;
                Data = Arrays.copyOf(Data, (Data.length / cfg.DataSize + 1) * cfg.DataSize);
            }
            for (int i = cfg.InitSeqNo; i <= LastToSend; ++i) {
                DataSegments.add(Arrays.copyOfRange(Data, i * cfg.DataSize, (i + 1) * cfg.DataSize));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public boolean SendFile() {
        while (true) {
            break;
        }
        return true;
    }

    public void SendPacket() {
        int offset = NextToSend - cfg.InitSeqNo;
        byte[] PDU = PDUFrame.SerializeFrame((byte) 0, (short) NextToSend, (short) AckReceived,
                DataSegments.get(offset));
        try {
            DatagramPacket PDUPacket = new DatagramPacket(PDU, PDU.length, InetAddress.getByName(IPAddress),
                    cfg.UDPPort);
            DatagramSocket DSocket = new DatagramSocket();
            DSocket.send(PDUPacket);
            DSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ++NextToSend;
    }
    /**
     * 
     * @return Data segment
     */
    public byte[] ReceivePacket() {
        return null;
    }
}
