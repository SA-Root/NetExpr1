package com.vinewood;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import com.vinewood.utils.RGBN_Config;

public class UDPCommInstance {
    private String IPAddress;
    private String FilePath;
    // For receiving data
    private int NextToReceive;
    private Object SyncNextToReceive;// lock
    // For sending data
    private int AckReceived;
    private Object SyncAckReceived;// lock
    // next index of DataSegments
    private int NextToSend;
    private Object SyncNextToSend;// lock
    private List<byte[]> DataSegments;
    private RGBN_Config cfg;
    private int PacketSize;
    private DatagramSocket UDPSocket;
    private Semaphore SlidingWindow;

    public UDPCommInstance(String ip_addr, RGBN_Config config) {
        IPAddress = ip_addr;
        cfg = config;
        AckReceived = cfg.InitSeqNo;
        NextToReceive = cfg.InitSeqNo + 1;
        NextToSend = cfg.InitSeqNo;
        PacketSize = cfg.DataSize + 9;
        try {
            UDPSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SyncAckReceived = new Object();
        SyncNextToReceive = new Object();
        SyncNextToSend = new Object();
    }

    private void ReadFile() {
        File f = new File(FilePath);
        try {
            FileInputStream fStream = new FileInputStream(f);
            byte[] Data = fStream.readAllBytes();
            fStream.close();
            int LastToSend;
            if (Data.length % cfg.DataSize == 0) {
                LastToSend = Data.length / cfg.DataSize + cfg.InitSeqNo - 1;
            }
            // fill 0
            else {
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

    public void SendFileThread() {
        while (true) {
            break;
        }
        UDPSocket.close();
    }

    /**
     * entering this method means ready to send. Accesses NextToSend,AckReceived
     */
    public void SendPacket() {
        // lock entire method for NextToSend,AckReceived
        int offset = NextToSend - cfg.InitSeqNo;
        byte[] PDU = PDUFrame.SerializeFrame((byte) 0, (short) NextToSend, (short) AckReceived,
                DataSegments.get(offset));
        try {
            DatagramPacket PDUPacket = new DatagramPacket(PDU, PDU.length, InetAddress.getByName(IPAddress),
                    cfg.UDPPort);
            UDPSocket.send(PDUPacket);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ++NextToSend;
    }

    public void ReceiveThread() {
        byte[] buffer = new byte[4096];
        DatagramPacket ReceivedPacket;
        while (!UDPSocket.isClosed()) {
            ReceivedPacket = new DatagramPacket(buffer, buffer.length);

        }
    }

    /**
     * launch method, runs on main thread
     */
    public void run() {
        Thread TReceive = new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveThread();
            }
        });
        TReceive.start();
        Scanner InputScanner = new Scanner(System.in);
        while (true) {
            System.out.print("File to send('quit' to exit): ");
            String path = InputScanner.next();
            if (path == "quit") {
                break;
            }
            FilePath = path;
            File FileToSend = new File(FilePath);
            if (FileToSend.exists()) {
                ReadFile();
                SlidingWindow = new Semaphore(cfg.SWSize);
                Thread TSendFile = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SendFileThread();
                    }
                });
                TSendFile.start();
            } else {
                System.out.println("[ERROR]Invalid file path.");
            }
        }
        InputScanner.close();
        UDPSocket.close();
    }
}
