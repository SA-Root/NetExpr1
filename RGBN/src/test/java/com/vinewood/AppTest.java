package com.vinewood;

import com.vinewood.utils.ChecksumMismatchException;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
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
