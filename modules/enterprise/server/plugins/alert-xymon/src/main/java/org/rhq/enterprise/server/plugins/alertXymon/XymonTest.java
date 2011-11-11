package org.rhq.enterprise.server.plugins.alertXymon;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 1/17/11
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class XymonTest {
    public static void main(String[] args) {
        BufferedReader reader = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/home/torben/apps/xymon/bb", "--debug", "cortal.virt.lan", getMessage());
            processBuilder.redirectErrorStream(true);
            Process proc = processBuilder.start();
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(proc.getInputStream())));
            String line;
            do {
                line = reader.readLine();
                System.out.println(line);
            } while (line != null);
            System.out.println("Exit value: " + proc.waitFor());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    private static String getMessage() {
        return "status jon,virt,lan.jboss-Test green Thu Nov 18 04:05:55 2010 jon" +
                "<table summary='Status summary' border=1>\n" +
                "        <tr>\n" +
                "            <td valign=top>&green</td>\n" +
                "            <td valign=top>Thu Nov 18 04:05:55 2010</td>\n" +
                "            <td>JON Server Availability<br/>\n" +
                "            Resource: <a href='https://10.204.194.33:9443/rhq/resource/summary/overview.xhtml?id=0'>null</a></td>\n" +
                "        </tr></table>";
    }
}
