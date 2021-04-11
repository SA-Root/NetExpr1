package com.vinewood;

import java.io.*;
import com.alibaba.fastjson.JSON;
import com.vinewood.utils.RGBN_Config;
import com.vinewood.utils.ChecksumMismatchException;

public class RGBN {
    private static RGBN_Config config;

    /**
     * Load config.json
     * 
     * @return 0: normal, 1: error
     */
    private static int LoadConfig() {
        File cfg = new File("sideload/config.json");
        try {
            FileInputStream fStream = new FileInputStream(cfg);
            config = JSON.parseObject(fStream, RGBN_Config.class);

            fStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    /**
     * Main func
     * 
     * @param args file to send
     */
    public static void main(String[] args) {

        // Test
        byte[] dat = new byte[] { 1, 2, 42, 6, 119, 4, 119, 119, 5 };
        byte[] res = PDUFrame.SerializeFrame((byte) 2, (short) 3, (short) 4, dat);
        PDUFrame ret = null;
        try {
            ret = PDUFrame.DeserializeFrame(res);
        } catch (ChecksumMismatchException e) {
            e.printStackTrace();
        }
        System.out.println(ret.Data);
    }
}
