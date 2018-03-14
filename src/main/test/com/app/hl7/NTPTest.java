//package com.app.hl7;
//
//import org.apache.commons.net.ntp.NTPUDPClient;
//import org.apache.commons.net.ntp.NtpUtils;
//import org.apache.commons.net.ntp.TimeInfo;
//import org.apache.commons.net.ntp.TimeStamp;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.openhealthexchange.openpixpdq.util.DateUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.TimeZone;
//
///**
// * 从NTF服务器获取时间并且同步本地时间
// * 注意： 因为所有的类型的服务器修改时间操作均需要root权限，所以单元测试需要以root权限启动测试
// */
//public class NTPTest {
//    private static final Logger logger = LoggerFactory.getLogger(NTPTest.class);
//    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    private static String serverTime;
//    private static String localTime = smartFormtTime(new Date().getTime());
//    private static  String osType;
//    private static String ntpServerAddress = "time-a.nist.gov";
//
//    @Before
//    public void before() {
//        logger.info("Test time NTP, now time is {}", localTime);
//        osType = getOsType();
//        logger.info("Get os type is {}", osType);
//    }
//
//
//    @Test
//    public void start(){
//        // 1: 从NTP服务器获取东八区时间
//        serverTime = getDateTimeFromNtpServer();
//        logger.info("get date time from NTP server is: {}", serverTime);
//        localTimeSynchronization();
//    }
//
//
//    @After
//    public void after() {
//        logger.info("Synchronization local system time end!, time is: {}", serverTime);
//    }
//
//
//    @Ignore("与NTP时间服务器同步时间方法")
//    public String getDateTimeFromNtpServer() {
//        String ntpAddress = "cn.ntp.org.cn";
//        return getDateTimeFromNtpServer(ntpAddress);
//    }
//
//    @Test
//    public void testLwNtpServer(){
//        getDateTimeFromNtpServer("us.ntp.org.cn");
//    }
//
//    @Test
//    public void testRuntime(){
//        Process process = null;
//        String shpath="/Users/xuning/workspace/idea/App/script/sync.sh";
//        String command = "/bin/sh " + shpath;
//        List<String> processList = new ArrayList<String>();
//        try {
//            process = Runtime.getRuntime().exec(command);
//            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line = "";
//            while ((line = input.readLine()) != null) {
//                processList.add(line);
//            }
//            input.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        for (String line : processList) {
//            System.out.println(line);
//        }
//    }
//
//    @Test
//    public void customtest() throws IOException {
//        String TIME_SERVER = "gazelle.ihe-c.org";
//        NTPUDPClient timeClient = new NTPUDPClient();
//        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
//        TimeInfo timeInfo = timeClient.getTime(inetAddress);
//        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
//        Date time = new Date(returnTime);
//        System.out.println("Time from " + TIME_SERVER + ": " + time.getTime());
//        System.out.println("Time: "+(returnTime-new Date().getTime()));
//    }
//    @Ignore("自定义地址与NTP时间服务器同步时间方法")
//    public String getDateTimeFromNtpServer(String ntpAddress){
//        try {
//            NTPUDPClient timeClient = new NTPUDPClient();
//            InetAddress timeServerAddress = InetAddress.getByName(ntpAddress);
//            TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
//            TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp();
//            Long nowTime = new Date().getTime();
//            logger.info("Now date {}, ntp server data: {}", nowTime,timeStamp.getDate().getTime());
//            return dateFormat.format(timeStamp.getDate());
//        } catch (Throwable e) {
//            logger.error("与ntp服务器同步时间错误！", e);
//        }
//        return null;
//    }
//
//
//
//    @Ignore("处理ntp时间与本地时间同步主逻辑")
//    public void localTimeSynchronization(){
//        resetTimeByOsType();
//    }
//
//
//    @Ignore("获取操作系统的类型")
//    public static final String getOsType(){
//        String osName = System.getProperty("os.name");
//        if (osName.equalsIgnoreCase("Mac OS X")){
//            return "MAC";
//        }else if (osName.equalsIgnoreCase("win")){
//            return "WIN";
//        }else {
//            return "LINUX";
//        }
//    }
//
//    @Ignore("重启本地时间为ntp时间")
//    public void resetTimeByOsType(){
//        String cmd = "";
//        try {
//            if (osType.equalsIgnoreCase("WIN")) {// Window 系统
//                // 格式 HH:mm:ss
//                cmd = "cmd /c time "+serverTime.substring(11, 8);
//                Runtime.getRuntime().exec(cmd);
//                // 格式：yyyy-MM-dd
//                cmd = "cmd /c date "+serverTime.substring(0, 10);
//                Runtime.getRuntime().exec(cmd);
//            } else if (osType.equalsIgnoreCase("LINUX")){// Linux 系统
//                // 格式：yyyyMMdd
//                cmd = "date -s "+serverTime+" && hwclock --systohc";
//                Runtime.getRuntime().exec(cmd);
//            }else {
//                cmd = "sudo date "+serverTime ;
//                Runtime.getRuntime().exec(cmd);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Ignore("格式化时间为字符表示类型")
//    public static final String smartFormtTime(long time){
//        return dateFormat.format(time);
//    }
//}
