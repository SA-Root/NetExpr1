package com.vinewood;

import java.io.*;
import java.util.Scanner;

import com.vinewood.utils.RGBN_Config;
import com.vinewood.utils.ChecksumMismatchException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(cfg, RGBN_Config.class);
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
        LoadConfig();
        Scanner sc = new Scanner(System.in);
        String IPAddr = sc.nextLine();
        UDPCommInstance UCI = new UDPCommInstance(IPAddr, config);
        UCI.run();
    }
}
