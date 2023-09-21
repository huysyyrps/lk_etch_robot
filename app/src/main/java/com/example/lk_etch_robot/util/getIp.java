package com.example.lk_etch_robot.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * 连接当前热点的设备的IP
 */
public class getIp {
    String address;
    public String getConnectIp() throws Exception {
        ArrayList<String> connectIpList = new ArrayList<String>();
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec("ip neigh show");
        proc.waitFor();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//        BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
        String line;
        while ((line = br.readLine()) != null) {
            String[] splitted = line.split(" +");
            if (splitted != null && splitted.length >= 4) {
                String ip = splitted[0];
                String newString = ip.toString().replace(".","=");
                String[] strs = newString.split("=");
                if (strs.length>3&&strs[2].equals("43")){
                    connectIpList.add(ip);
                }
            }
        }
        for (int i=0;i<connectIpList.size();i++){
            String ip = connectIpList.get(i);
            ip = ip.replace(".",",");
            String[] all=ip.split(",");
            if (all[2].equals("43"));
            address = connectIpList.get(i);
            break;
        }
        return address;
    }
}
