package com.incar.bickpanel;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void ssr(){
        byte u1 = 0x17;
        byte u2 = 0x00;

        System.out.println(((u1&0xffff)<<8)+u2);
    }
}