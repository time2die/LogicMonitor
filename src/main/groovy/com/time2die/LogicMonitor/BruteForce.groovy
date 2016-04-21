package com.time2die.LogicMonitor

/**
 * Created by time2die on 21.04.16.
 */
class BruteForce {
    public static void main(String[] args) {

        int i = 286331152;
        String hex = "";
        while (true) {
            i++
            hex = Integer.toHexString(i)
            println "$hex"
            def echo = """wget https://pp.vk.me/c5876/u2633465/154639050/w_$hex""".execute()
            echo.waitFor()
            println "${echo}"
            if (hex.length() == 9) break
        }
    }

}

