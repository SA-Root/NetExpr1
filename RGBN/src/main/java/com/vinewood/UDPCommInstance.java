package com.vinewood;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.vinewood.utils.RGBN_Config;

class TimeoutPack {
    int PacketNo;
    Date SendDate;

    public TimeoutPack(int pkg_no, Date sdate) {
        PacketNo = pkg_no;
        SendDate = sdate;
    }
}

/**
 * @author Guo Shuaizhe
 */
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
    private ArrayList<byte[]> DataSegments;
    private RGBN_Config cfg;
    private int PacketSize;
    private int FileLength;
    private DatagramSocket UDPSocket;
    private Semaphore SlidingWindow;
    private Thread TReceive;
    private Thread TReceiveData;
    private Thread TReceiveAck;
    private ConcurrentLinkedQueue<PDUFrame> RecvDataQueue;
    private ConcurrentLinkedQueue<PDUFrame> RecvAckQueue;
    private LinkedList<TimeoutPack> TimeoutQueue;

    public UDPCommInstance(RGBN_Config config) {
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
        DataSegments = new ArrayList<byte[]>();
        try {
            FileInputStream fStream = new FileInputStream(f);
            byte[] Data = fStream.readAllBytes();
            FileLength = Data.length;
            fStream.close();
            int LastToSend;
            if (Data.length % cfg.DataSize == 0) {
                LastToSend = Data.length / cfg.DataSize + cfg.InitSeqNo - 1;
                for (int i = 0; i <= LastToSend - cfg.InitSeqNo; ++i) {
                    DataSegments.add(Arrays.copyOfRange(Data, i * cfg.DataSize, (i + 1) * cfg.DataSize));
                }
            }
            // fill 0
            else {
                LastToSend = Data.length / cfg.DataSize + cfg.InitSeqNo;
                int FilledLength = (Data.length / cfg.DataSize + 1) * cfg.DataSize;
                byte[] FilledData = new byte[FilledLength];
                FilledData = Arrays.copyOf(Data, FilledLength);
                for (int i = 0; i <= LastToSend - cfg.InitSeqNo; ++i) {
                    DataSegments.add(Arrays.copyOfRange(FilledData, i * cfg.DataSize, (i + 1) * cfg.DataSize));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void SendFileThread() {
        // initialize
        RecvDataQueue = new ConcurrentLinkedQueue<PDUFrame>();
        RecvAckQueue = new ConcurrentLinkedQueue<PDUFrame>();
        SlidingWindow = new Semaphore(cfg.SWSize);
        TimeoutQueue = new LinkedList<TimeoutPack>();
        // send file length first
        try {
            SlidingWindow.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        byte[] StrLength = ByteBuffer.allocate(4).putInt(FileLength).array();
        byte[] PDU = PDUFrame.SerializeFrame((byte) 0, (short) NextToSend, (short) AckReceived, StrLength);
        try {
            DatagramPacket PDUPacket = new DatagramPacket(PDU, PDU.length, InetAddress.getByName(IPAddress),
                    cfg.UDPPort);
            UDPSocket.send(PDUPacket);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ++NextToSend;
        // send real file
        while (true) {
            try {
                SlidingWindow.acquire();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            break;
        }
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
            try {
                UDPSocket.receive(ReceivedPacket);
                byte[] bPDU = ReceivedPacket.getData();
                PDUFrame PDU = PDUFrame.DeserializeFrame(bPDU);
                switch (PDU.FrameType) {
                // data frame
                case 0:

                    break;
                // ack frame
                case 1:

                    break;
                // nak frame
                case 2:
                    break;
                // header frame
                case 3:
                    break;
                default:
                    break;
                }
            } catch (Exception e) {

            }
        }
    }

    private void ReceiveDataThread() {

    }

    private void ReceiveAckThread() {

    }

    /**
     * launch method, runs on main thread
     */
    public void run() {
        System.out.print("IP Address to communicate: ");
        Scanner InputScanner = new Scanner(System.in);
        IPAddress = InputScanner.nextLine();
        // launch receive thread
        TReceive = new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveThread();
            }
        });
        TReceive.start();
        // launch receive data thread
        TReceiveData = new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveDataThread();
            }
        });
        TReceiveData.start();
        // launch receive ack thread
        TReceiveAck = new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveAckThread();
            }
        });
        TReceiveAck.start();
        while (true) {
            System.out.print("File to send('quit' to exit): ");
            String path = InputScanner.nextLine();
            if (path.equals("quit")) {
                break;
            }
            FilePath = path;
            File FileToSend = new File(FilePath);
            if (FileToSend.exists()) {
                ReadFile();
                SendFileThread();
            } else {
                System.out.println("[ERROR]Invalid file path.");
            }
        }
        InputScanner.close();
        UDPSocket.close();
        System.out.println("Socket on " + IPAddress + ":" + Integer.toString(cfg.UDPPort) + " is closed.");
    }
}
