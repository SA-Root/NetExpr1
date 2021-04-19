package com.vinewood;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.vinewood.utils.ChecksumMismatchException;
import com.vinewood.utils.CrcUtil;
import com.vinewood.utils.RGBN_Config;
import com.vinewood.utils.RGBN_Utils;

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
    private RGBN_Config cfg;
    private int PacketSize;
    private DatagramSocket UDPSocket;
    private Thread TReceive;
    // -------------------send----------------------
    private Semaphore SlidingWindow;
    private String IPAddress;
    private String FilePath;
    private int AckReceived;
    private Object SyncAckReceived;// lock
    // next index of DataSegments
    private int NextToSend;
    private Object SyncNextToSend;// lock
    private int LastToSend;
    private ArrayList<byte[]> DataSegments;
    private int SendFileLength;
    private Thread TReceiveAck;
    private ConcurrentLinkedQueue<PDUFrame> RecvAckQueue;
    private LinkedList<TimeoutPack> TimeoutQueue;
    private FileOutputStream LogFileSend;
    private int TotalSentDataPDUCount;
    private int TotalTimeoutPDUCount;
    private int TotalRetransmissionCount;
    // -------------------end send--------------------
    // ---------------------receive---------------------
    private int NextToReceive;
    private byte[][] DataReceived;
    private int ReceiveFileLength;
    private Thread TReceiveData;
    private ConcurrentLinkedQueue<PDUFrame> RecvDataQueue;
    private FileOutputStream LogFileReceive;
    private int TotalReceivedDataPDUCount;
    // -------------------end receive---------------------

    public UDPCommInstance(RGBN_Config config) {
        cfg = config;
        AckReceived = cfg.InitSeqNo;
        NextToReceive = cfg.InitSeqNo;
        NextToSend = cfg.InitSeqNo;
        PacketSize = cfg.DataSize + 9;
        try {
            UDPSocket = new DatagramSocket(cfg.UDPPort);
            System.out.printf("[INFO]UDP Port on %d opened.\n", cfg.UDPPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SyncAckReceived = new Object();
        SyncNextToSend = new Object();
    }

    private void ReadFile() {
        File f = new File(FilePath);
        DataSegments = new ArrayList<byte[]>();
        try {
            FileInputStream fStream = new FileInputStream(f);
            byte[] Data = fStream.readAllBytes();
            SendFileLength = Data.length;
            fStream.close();
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

    private void CheckTimeout() {
        if (!TimeoutQueue.isEmpty()) {
            TimeoutPack head = TimeoutQueue.getFirst();
            Date now = new Date();
            // pkg already acked
            if (head.PacketNo < AckReceived) {
                TimeoutQueue.removeFirst();
                while (!TimeoutQueue.isEmpty() && TimeoutQueue.getFirst().PacketNo < AckReceived) {
                    TimeoutQueue.removeFirst();
                }
            } else {
                // timeout
                if (now.getTime() - head.SendDate.getTime() >= cfg.Timeout && head.PacketNo == AckReceived) {
                    synchronized (SyncNextToSend) {
                        synchronized (SyncAckReceived) {
                            NextToSend = head.PacketNo;
                            AckReceived = head.PacketNo;
                        }
                    }
                    TimeoutQueue.clear();
                }
            }
        }
    }

    private void SendSendFileLength() {
        try {
            SlidingWindow.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        byte[] StrLength = ByteBuffer.allocate(cfg.DataSize).putInt(SendFileLength).array();
        synchronized (SyncNextToSend) {
            synchronized (SyncAckReceived) {
                byte[] PDU = PDUFrame.SerializeFrame((byte) 3, (short) NextToSend, (short) AckReceived, StrLength);
                try {
                    DatagramPacket PDUPacket = new DatagramPacket(PDU, PDU.length, InetAddress.getByName(IPAddress),
                            cfg.UDPPort);
                    UDPSocket.send(PDUPacket);
                    // write log
                    String logLine = String.format("[SEND]%d,pdu_to_send=%d,status=New,ackedNo=%d\n",
                            ++TotalSentDataPDUCount, NextToSend, AckReceived);
                    System.out.print(logLine);
                    LogFileSend.write(logLine.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                ++NextToSend;
            }
        }
    }

    public void SendFileThread() {
        Instant s = Instant.now();
        // initialize
        RecvDataQueue = new ConcurrentLinkedQueue<PDUFrame>();
        RecvAckQueue = new ConcurrentLinkedQueue<PDUFrame>();
        SlidingWindow = new Semaphore(cfg.SWSize);
        TimeoutQueue = new LinkedList<TimeoutPack>();
        TotalSentDataPDUCount = 0;
        TotalTimeoutPDUCount = 0;
        TotalRetransmissionCount = 0;
        // create log file
        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss a");
        File directory = new File(".");
        try {
            String pwd = directory.getCanonicalPath();
            File LogF = new File(pwd + "/log/" + ft.format(now) + " Send.log");
            LogF.createNewFile();
            LogFileSend = new FileOutputStream(LogF);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // send file length first
        String logLine = String.format("[INFO]Send file '%s' to %s:%d,size=%d Bytes\n", FilePath, IPAddress,
                cfg.UDPPort, SendFileLength);
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SendSendFileLength();
        // send real file
        while (true) {
            try {
                SlidingWindow.acquire();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            CheckTimeout();
            synchronized (SyncNextToSend) {
                synchronized (SyncAckReceived) {
                    SendPacket();
                    if (NextToSend > LastToSend) {
                        break;
                    }
                }
            }
        }
        Instant end = Instant.now();
        SendFileStatistics(s, end);
        // close
        try {
            LogFileSend.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SendFileStatistics(Instant s, Instant end) {
        // statistics
        String logLine = String.format("[INFO]File '%s' successfully sent to %s:%d\n", FilePath, IPAddress,
                cfg.UDPPort);
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logLine = String.format("[INFO]Total PDU count:%d\n", TotalSentDataPDUCount);
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logLine = String.format("[INFO]Total timeout:%d\n", TotalTimeoutPDUCount);
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logLine = String.format("[INFO]Total retransmission:%d\n", TotalRetransmissionCount);
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logLine = String.format("[INFO]Time elapsed:%d\n", Duration.between(s, end).getSeconds());
        System.out.print(logLine);
        try {
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
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
            // write log
            String logLine = String.format("[SEND]%d,pdu_to_send=%d,status=New,ackedNo=%d\n", ++TotalSentDataPDUCount,
                    NextToSend, AckReceived);
            System.out.print(logLine);
            LogFileSend.write(logLine.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        TimeoutQueue.add(new TimeoutPack(NextToSend, new Date()));
        ++NextToSend;
    }

    public void ValidatePacket(PDUFrame p) throws ChecksumMismatchException {
        int original = p.Checksum;
        int now = RGBN_Utils.ShortFromByteArray(CrcUtil.GetCRC16(p.Data), 0);
        if (original != now) {
            throw new ChecksumMismatchException();
        }
    }

    public void ReceiveThread() {
        byte[] buffer = new byte[PacketSize];
        DatagramPacket ReceivedPacket;
        while (!UDPSocket.isClosed()) {
            ReceivedPacket = new DatagramPacket(buffer, buffer.length);
            PDUFrame PDU = null;
            try {
                UDPSocket.receive(ReceivedPacket);
                byte[] bPDU = ReceivedPacket.getData();
                PDU = PDUFrame.DeserializeFrame(bPDU);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            switch (PDU.FrameType) {
            // data frame
            case 0:
                RecvDataQueue.add(PDU);
                synchronized (TReceiveData) {
                    TReceiveData.notify();
                }
                break;
            // ack frame
            case 1:
                // nak frame
            case 2:
                RecvAckQueue.add(PDU);
                synchronized (TReceiveAck) {
                    TReceiveAck.notify();
                }
                break;
            // header frame
            case 3:
                // create log file
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss a");
                File directory = new File(".");
                try {
                    String pwd = directory.getCanonicalPath();
                    File LogF = new File(pwd + "/log/" + ft.format(now) + " Receive.log");
                    LogF.createNewFile();
                    LogFileReceive = new FileOutputStream(LogF);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // validate packet
                try {
                    ValidatePacket(PDU);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                // analyze
                ReceiveFileLength = RGBN_Utils.IntFromByteArray(PDU.Data, 0);
                if (ReceiveFileLength % cfg.DataSize == 0) {
                    DataReceived = new byte[ReceiveFileLength / cfg.DataSize][cfg.DataSize];
                } else {
                    DataReceived = new byte[ReceiveFileLength / cfg.DataSize + 1][cfg.DataSize];
                }
                // write log
                String logLine = String.format("[INFO]Receive file from %s,port=%d,size=%d Bytes\n",
                        ReceivedPacket.getAddress().toString(), cfg.UDPPort, ReceiveFileLength);
                System.out.print(logLine);
                try {
                    LogFileReceive.write(logLine.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                logLine = String.format("[RECEIVE]%d,pdu_exp=%d,pdu_recv=%d,status=OK\n", ++TotalReceivedDataPDUCount,
                        NextToReceive++, PDU.SeqNo);
                System.out.print(logLine);
                try {
                    LogFileReceive.write(logLine.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
            }
        }
    }

    private void ReceiveDataThread() {
        // close logfilereceive after receiving
        synchronized (TReceiveData) {
            
            try {
                TReceiveData.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void ReceiveAckThread() {
        synchronized (TReceiveAck) {

            try {
                TReceiveAck.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        System.out.println("[INFO]UDP Port on " + Integer.toString(cfg.UDPPort) + " is closed.");
    }
}
