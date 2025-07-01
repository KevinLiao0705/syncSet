package base3;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

//asterisk sound location: usr/share/asterisk/sounds/en/
public class ConsoleMain {

    static ConsoleMain scla;
    Timer tm1 = null;
    ConsoleMainCmdExe cexe;
    Map<String, CmdTask> taskMap;
    Map<String, ChkRxA> rxMap;
    Map<String, CmdStatus> cmdStaMap;
    //SlotSta[] slotStaA = new SlotSta[16];
    int ctrIoPort = 23501;
    int myDeviceId = 0x2403;
    int devicePcioId = 0x2301;
    int mySerialId = 0x0000;
    String preCmdStr = "";
    KvComm ioComm;
    Uart uart0 = new Uart();
    SyncData syncData = new SyncData();
    int appId = 0;
    int easyCommand = 0;
    int easyParas = 0;
    int easyCommandTime = 0;
    int testUartTime = 0;
    int connectFpgaCnt = 0;
    int[] drvDataClrBuf = new int[36];
    int addBufDebugCnt = 0;

    //===========================
    //dataToFpga
    /*
    u16 deviceId=25011
    u16 serialId=appId
    u16 groupId=0xAB00;
    u16 cmdLen(10+payload len)  
    u16 cmd
    u16 para0
    u16 para1
    u16 para2
    u16 para3
    paload
     */
    //command
    //command 1000:
    //* [openSspaPower -1]: open all enable sspaPower;
    //      [openSspaPower %inx]: open enable sspaPower of serial inx ;
    //* [closeSspaPower -1]: close all sspaPower;
    //      [closeSspaPower %inx]: close sspaPower of serial inx ;
    //* [openSspaModule -1]: open all enable sspaModule;
    //      [openSspaPower %inx]: open enable sspaModule of serial inx ;
    //* [closeSspaModule -1]: close all sspaModule;
    //      [closeSspaPower %inx]: close sspaModule of serial inx ;
    static public JSONObject wsCallBack(String userName, JSONObject mesJson, String actStr, JSONObject outJson) {
        try {
            String act = (String) mesJson.get("act");
            if (act.equals("tick")) {
                scla.transSyncData(outJson);
                return outJson;
            }
            
            
            
            Object obj = null;
            try {
                obj = mesJson.get("paras");
            } catch (Exception ex) {

            }
            JSONArray paras = null;
            if (obj != null) {
                paras = (JSONArray) obj;
            }

            outJson.put("status", "ok");
            if (scla.appId == 0) {
                if (act.equals("mastPulseEnable")) {
                    scla.setEasyCommand(0x200b, paras);
                    outJson.put("status", "ok");
                    return outJson;
                }
                if (act.equals("mastPulseDisable")) {
                    scla.setEasyCommand(0x200c, paras);
                    outJson.put("status", "ok");
                    return outJson;
                }

            }

            if (act.equals("closeUi")) {
                scla.cmdFunc("simulateKeyExit");                
                return outJson;
                
            }
            
            
            String preText = "";
            int preInx = 0;
            int status0 = 0;
            int shift = 0;
            byte[] powerStatusA = null;
            byte[] moduleStatusA = null;
            if (scla.appId == 1) {
                preText = "sub1";
                preInx = 0;
                shift = 22;
                status0 = scla.syncData.systemStatus0 >> 22;
                powerStatusA = scla.syncData.sspaPowerStatusAA;
                moduleStatusA = scla.syncData.sspaModuleStatusAA;
            }
            if (scla.appId == 2) {
                preText = "sub2";
                preInx = 1;
                shift = 27;
                status0 = scla.syncData.systemStatus0 >> 27;
                powerStatusA = scla.syncData.sspaPowerStatusAA;
                moduleStatusA = scla.syncData.sspaModuleStatusAA;

            }
            if (scla.appId == 3) {
                preText = "ctr1";
                preInx = 0;
                shift = 22;
                status0 = scla.syncData.systemStatus0 >> 22;
                powerStatusA = scla.syncData.sspaPowerStatusAA;
                moduleStatusA = scla.syncData.sspaModuleStatusAA;
            }
            if (scla.appId == 4) {
                preText = "ctr2";
                preInx = 1;
                shift = 27;
                status0 = scla.syncData.systemStatus0 >> 27;
                powerStatusA = scla.syncData.sspaPowerStatusAA;
                moduleStatusA = scla.syncData.sspaModuleStatusAA;

            }
            int powerOn_f;
            int moduleOn_f;
            for (int i = 0; i < 36; i++) {
                if (((powerStatusA[i] >> 4) & 1) == 1) {
                    powerOn_f = 1;
                }
                if (((moduleStatusA[i] >> 1) & 1) == 1) {
                    moduleOn_f = 1;
                }

            }
            int emergency = scla.syncData.systemStatus0 & (1 << (shift + 4));
            int ready_f = scla.syncData.systemStatus0 & 3;

            if (act.equals(preText + "SspaPowerOn")) {
                if (GB.emulate == 2) {
                    if ((ready_f != 2) || (emergency != 0)) {
                        return outJson;
                    }
                    obj = paras.get(0);
                    int index = (int) obj;
                    if (index >= 0) {
                        powerStatusA[index] |= 0x1d;
                        return outJson;
                    }
                    for (int i = 0; i < 36; i++) {
                        powerStatusA[i] |= 0x1d;
                    }
                    return outJson;
                }
                if (GB.emulate == 0) {
                    scla.setEasyCommand(0x2000, paras);
                }
                return outJson;
            }
            if (act.equals(preText + "SspaPowerOff")) {
                if (GB.emulate == 2) {
                    obj = paras.get(0);
                    int index = (int) obj;
                    if (index >= 0) {
                        powerStatusA[index] &= 0xffffffe2;
                        return outJson;
                    }
                    for (int i = 0; i < 36; i++) {
                        powerStatusA[i] &= 0xffffffe2;
                    }
                }

                if (GB.emulate == 0) {
                    scla.setEasyCommand(0x2001, paras);
                }
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "SspaModuleOn")) {
                if (GB.emulate == 1) {
                }
                scla.setEasyCommand(0x2002, paras);
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "SspaModuleOff")) {
                if (GB.emulate == 1) {
                }
                scla.setEasyCommand(0x2003, paras);
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "RadiationOn")) {
                scla.setEasyCommand(0x2004, paras);
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "RadiationOff")) {
                scla.setEasyCommand(0x2005, null);
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "EmergencyOn")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x2006, null);
                return outJson;
            }
            if (act.equals(preText + "EmergencyOff")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x2007, null);
                return outJson;
            }
            if (act.equals("selfTestStartAll")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x2008, null);
                return outJson;
            }
            if (act.equals("selfTestStopAll")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x2009, null);
                return outJson;
            }
            if (act.equals("selfTestSlot")) {
                scla.setEasyCommand(0x200a, paras);
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals(preText + "PulseSource")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x200d, paras);
                return outJson;
            }
            if (act.equals(preText + "TxLoad")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x200e, paras);
                return outJson;
            }
            if (act.equals(preText + "BatShort")) {
                outJson.put("status", "ok");
                scla.setEasyCommand(0x200f, paras);
                return outJson;
            }

        } catch (Exception ex) {

        }
        return outJson;

    }

    public void setEasyCommand(int cmd, JSONArray paras) {

        if (paras == null) {
            easyParas = 0;
            easyCommand = cmd;
            easyCommandTime = 0;
        } else {
            try {
                easyParas = (int) paras.get(0);
                easyCommand = cmd;
                easyCommandTime = 0;
            } catch (Exception ex) {
                return;
            }
        }

    }

    public void transSyncData(JSONObject outJson) {
        try {
            if (appId == 0) {
                KvJson kj = new KvJson();
                kj.jStart();
                kj.jadd("slotDataAA#" + (appId), syncData.slotDataAA);
                kj.jadd("systemStatus0", syncData.systemStatus0);
                kj.jadd("systemStatus1", syncData.systemStatus1);
                kj.jadd("gngga0", syncData.gngga0);
                kj.jadd("gngga1", syncData.gngga1);
                kj.jadd("gngga2", syncData.gngga2);
                kj.jadd("viewDatas", syncData.viewDatas);
                kj.jEnd();
                JSONObject syncJson = new JSONObject(kj.jstr);
                outJson.put("syncData", syncJson);
            }

            if (appId == 1 || appId == 2) {
                KvJson kj = new KvJson();
                kj.jStart();
                kj.jadd("slotDataAA#" + (appId), syncData.slotDataAA);
                kj.jadd("systemStatus0", syncData.systemStatus0);
                kj.jadd("systemStatus1", syncData.systemStatus1);
                if (appId == 1) {
                    kj.jadd("gngga1", syncData.gngga1);
                }
                if (appId == 2) {
                    kj.jadd("gngga2", syncData.gngga2);
                }
                kj.jadd("viewDatas", syncData.viewDatas);
                kj.jEnd();
                JSONObject syncJson = new JSONObject(kj.jstr);
                outJson.put("syncData", syncJson);
            }

            if (appId == 3 || appId == 4) {
                KvJson kj = new KvJson();
                kj.jStart();
                kj.jadd("slotDataAA", syncData.slotDataAA);
                kj.jadd("systemStatus0", syncData.systemStatus0);
                kj.jadd("systemStatus1", syncData.systemStatus1);
                kj.jadd("enviStatusA", syncData.enviStatusA);
                kj.jadd("meterStatusAA", syncData.meterStatusAA);
                kj.jadd("sspaPowerStatusAA", syncData.sspaPowerStatusAA);
                kj.jadd("sspaPowerV50vAA", syncData.sspaPowerV50vAA);
                kj.jadd("sspaPowerV50iAA", syncData.sspaPowerV50iAA);
                kj.jadd("sspaPowerV50tAA", syncData.sspaPowerV50tAA);
                kj.jadd("sspaPowerV32vAA", syncData.sspaPowerV32vAA);
                kj.jadd("sspaPowerV32iAA", syncData.sspaPowerV32iAA);
                kj.jadd("sspaPowerV32tAA", syncData.sspaPowerV32tAA);
                kj.jadd("sspaModuleStatusAA", syncData.sspaModuleStatusAA);
                kj.jadd("sspaModuleRfOutAA", syncData.sspaModuleRfOutAA);
                kj.jadd("sspaModuleTemprAA", syncData.sspaModuleTemprAA);
                kj.jadd("viewDatas", syncData.viewDatas);
                //=================================
                /*
                syncData.pulseFormAddBufA0Len=6;
                syncData.pulseFormAddBufA0[0]=100000*2+1;
                syncData.pulseFormAddBufA0[1]=900000*2+0;
                syncData.pulseFormAddBufA0[2]=123000*2+1;
                syncData.pulseFormAddBufA0[3]=456000*2+0;
                syncData.pulseFormAddBufA0[4]=323000*2+1;
                syncData.pulseFormAddBufA0[5]=956000*2+0;
                 */
                int[] intA = new int[256];
                int pinx = 0;
                for (;;) {
                    int chgInx = (syncData.pulseFormAddBufA0Inx0 ^ syncData.pulseFormAddBufA0Inx1) & 255;
                    if (chgInx == 0) {
                        break;
                    }
                    intA[pinx++] = syncData.pulseFormAddBufA0[syncData.pulseFormAddBufA0Inx1 & 255];
                    syncData.pulseFormAddBufA0Inx1++;
                    if (pinx >= 256);
                    break;
                }
                kj.jadd("pulseFormAddBufA0", intA, pinx);
                intA[0] = syncData.pulseFormLowPeriod;
                intA[1] = syncData.pulseFormHighPeriod;
                intA[2] = syncData.pulseFormFreq;
                kj.jadd("pulseFormInf", intA, 3);

                kj.jEnd();
                JSONObject syncJson = new JSONObject(kj.jstr);
                syncData.pulseFormAddBufA0Len = 0;
                outJson.put("syncData", syncJson);
            }
        } catch (Exception ex) {

        }
    }

    /*
    int[] slotIdA = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    // 0:none, 1:ready, 2:error 3:warn up
    int[] slotStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    //0:none, 1:PreTest,2:testing;
    int[] slotTestStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    
    
    int[] mastSystemStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr1SystemStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr2SystemStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr1EnvStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr2EnvStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[][] ctr1SspaPowerStatusAA = new int[36][];
    int[][] ctr2SspaPowerStatusAA = new int[36][];
    int[][] ctr1SspaModuleStatusAA = new int[36][];
    int[][] ctr2SspaModuleStatusAA = new int[36][];
    int[] ctr1MeterStatusA = new int[]{0, 0, 0, 0, 0, 0};
    int[] ctr2MeterStatusA = new int[]{0, 0, 0, 0, 0, 0};
    int[] mastGpsDataA = new int[]{22, 59, 59, 99, 122, 59, 59, 99, 123, 270};
    String mastGpsStatus = "";
    int[] sub1GpsDataA = new int[]{22, 59, 59, 99, 122, 59, 59, 99, 123, 270};
    String sub1GpsStatusA = "";
    int[] sub2GpsDataA = new int[]{22, 59, 59, 99, 122, 59, 59, 99, 123, 270};
    String sub2GpsStatus = "";
    int[] mastRadarStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr1RadarStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] ctr2RadarStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] sub1CommStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int[] sub2CommStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
     */
    public ConsoleMain() {
        scla = this;
    }

    public void create() {
        String str;
        String errStr;
        //=======================================================
        try {
            String content = Lib.readFile("paraSet.json");
            GB.paraSetMap.clear();
            //String content = stringBuilder.toString();
            JSONObject jsPara = new JSONObject(content);
            Iterator<String> it = jsPara.keys();
            while (it.hasNext()) {
                String key = it.next();
                GB.paraSetMap.put(key, jsPara.get(key));
            }
        } catch (Exception ex) {

        }

        final ConsoleMain cla = this;
        //KvWebSocketServer.serverStart();
        //=======================================================
        rxMap = new HashMap<String, ChkRxA>();
        taskMap = new HashMap<String, CmdTask>();
        cmdStaMap = new HashMap<String, CmdStatus>();
        cexe = new ConsoleMainCmdExe(cla, taskMap);
        //=======================================
        ioComm = new KvComm("ioComm", "serverSocket");
        ioComm.serverSocket.format = 1;
        ioComm.serverSocket.rxcon_ltim = 100;
        ioComm.serverSocket.port = ctrIoPort;
        ioComm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                gnRxPrg("", bytes, len);
                socketServerReturn();
                return null;

            }
        });
        ioComm.open();
        //=======================================
        if (cla.tm1 == null) {
            cla.tm1 = new Timer();
            tm1.schedule(new ConsoleMainTm1(cla), 1000, 20);
        }
        errStr = cmdFunc("openComPort");
        if (errStr != null) {
            System.out.println(errStr);
        } else {
            System.out.println("open com port ok.");
        }
        //=====================================
        System.out.println("ConsoleMain Ready.");
        while (true) {
            Scanner input = new Scanner(System.in);
            str = input.nextLine().trim();
            if (str.length() == 0) {
                continue;
            }
            errStr = cmdFunc(str);
            if (errStr != null) {
                System.out.println(errStr);
            }
        }
    }

    public int utxParaSet() {
        int deviceId = 25010;
        int serialId = 0;
        int groupId = 0xab00;
        int cmd = 0x1000;
        int para0 = (int) GB.paraSetMap.get("appId");
        int para1 = 0;
        int para2 = 0;
        int para3 = 0;
        return 0;

    }

    public String openUart0() {
        if (uart0.uartSeted_f != 0) {
            return null;
        }
        uart0.portName = "COM" + (int) GB.paraSetMap.get("uart0Port");
        if (GB.prgMode == 1) {
            uart0.portName = "ttyUSB0";
        }
        System.out.println(uart0.portName);

        uart0.boudrate = (int) GB.paraSetMap.get("uart0Boudrate");
        uart0.parity = "None";
        uart0.stopBit = 1;
        uart0.dataBit = 8;
        uart0.txEncMode = 1;
        uart0.rxEncMode = 1;
        //&w
        uart0.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                int inx = 0;
                ByteLook bk = new ByteLook(bytes);
                int deviceId = bk.lookShortInt();
                int serialId = bk.lookShortInt();
                int groupId = bk.lookShortInt();
                int groupLen = bk.lookShortInt();
                int cmd = bk.lookShortInt();
                int para0 = bk.lookShortInt();
                int para1 = bk.lookShortInt();
                int para2 = bk.lookShortInt();
                int para3 = bk.lookShortInt();
                if (deviceId != 25010 || serialId != 0x0000) {
                    return null;
                }
                int ibuf = 0;
                int ibuf0, ibuf1, ibuf2, ibuf3;
                if (cmd == 0x1000) {
                    //if (para0 == 3 || para0 == 4)//fpgaId
                    if (true)//fpgaId
                    {
                        connectFpgaCnt = 0;
                        for (int i = 0; i < 12; i++) {
                            syncData.slotDataAA[i] = bk.lookShort();
                        }

                        syncData.systemStatus0 = bk.lookInt();
                        syncData.systemStatus1 = bk.lookInt();

                        for (;;) {
                            ibuf = bk.lookByteInt();
                            if (ibuf == 0xcd) {
                                break;
                            }
                            if ((ibuf == 0xaa)) {
                                ibuf = bk.lookByteInt();
                                syncData.enviStatusA[0] = bk.lookInt();
                                for (int i = 0; i < 6; i++) {
                                    syncData.meterStatusAA[i] = bk.lookShort();
                                }
                                continue;
                            }

                            if (ibuf == 0xab) {
                                ibuf = bk.lookByteInt();
                                if (ibuf >= 36) {
                                    return null;
                                }
                                syncData.sspaPowerStatusAA[ibuf] = bk.lookByte();
                                syncData.sspaPowerV50vAA[ibuf] = bk.lookShort();
                                syncData.sspaPowerV50iAA[ibuf] = bk.lookShort();
                                syncData.sspaPowerV50tAA[ibuf] = bk.lookShort();
                                syncData.sspaPowerV32vAA[ibuf] = bk.lookShort();
                                syncData.sspaPowerV32iAA[ibuf] = bk.lookShort();
                                syncData.sspaPowerV32tAA[ibuf] = bk.lookShort();
                                syncData.sspaModuleStatusAA[ibuf] = bk.lookByte();
                                syncData.sspaModuleRfOutAA[ibuf] = bk.lookShort();
                                syncData.sspaModuleTemprAA[ibuf] = bk.lookShort();
                                drvDataClrBuf[ibuf] = 0;
                                continue;
                            }
                            if (ibuf == 0xac) {
                                ibuf = bk.lookByteInt();
                                if (ibuf >= 32) {
                                    return null;
                                }
                                for (int i = 0; i < 8; i++) {
                                    syncData.viewDatas[ibuf * 8 + i] = bk.lookInt();
                                }
                                continue;
                            }

                            if (ibuf == 0xad || ibuf == 0xae || ibuf == 0xaf) {
                                byte[] byteA = null;
                                if (ibuf == 0xad) {
                                    byteA = syncData.gngga0;
                                }
                                if (ibuf == 0xae) {
                                    byteA = syncData.gngga1;
                                }
                                if (ibuf == 0xaf) {
                                    byteA = syncData.gngga2;
                                }
                                ibuf = bk.lookByteInt();
                                if (ibuf >= 64) {
                                    return null;
                                }
                                for (int i = 0; i < ibuf; i++) {
                                    byteA[i] = bk.lookByte();
                                }
                                continue;
                            }

                            if (ibuf == 0xb0) {
                                ibuf = bk.lookByteInt();
                                if (ibuf >= 16) {
                                    break;
                                }
                                for (int i = 0; i < ibuf; i++) {
                                    syncData.pulseFormAddBufA0[syncData.pulseFormAddBufA0Inx0 & 255] = bk.lookInt();
                                    syncData.pulseFormAddBufA0Inx0++;
                                }
                                continue;
                            }

                            if (ibuf == 0xb1) {
                                ibuf = bk.lookByteInt();
                                if (ibuf != 9) {
                                    break;
                                }
                                syncData.pulseFormLowPeriod = bk.lookInt();
                                syncData.pulseFormHighPeriod = bk.lookInt();
                                syncData.pulseFormFreq = bk.lookByte();
                                continue;
                            }
                            break;

                        }

                    }

                }

                uart0.rxSerialCnt++;
                uart0.rxSerialCnt &= 0xffff;
                if (uart0.rxSerialCnt != para1) {
                    uart0.rxErrCnt++;
                }
                uart0.rxSerialCnt = para1;
                uart0.rxPackageCnt++;
                if ((uart0.rxPackageCnt
                        % 100) == 0) {
                    System.out.print(" uart0Rx-" + uart0.rxErrCnt);
                    if ((uart0.rxPackageCnt % 1000) == 0) {
                        System.out.print("\n");
                    }
                }
                //===============================================================
                String preText;
                String preText1;

                try {
                    if (cmd == 0x1000) {//tick
                        uart0.txDeviceId = deviceId;
                        uart0.txSerialId = serialId;
                        uart0.txGroupId = 0xac00;
                        uart0.txCmd = cmd;
                        uart0.txPara0 = para0;  //fpgaId
                        uart0.txPara1 = para1;  //serialCnt
                        uart0.txPara2 = easyCommand; //easy command 
                        uart0.txPara3 = easyParas; //eaay command paras
                        uart0.txBufferLen = 0;
                        //================================================
                        if (easyCommand != 0) {
                            int xibuf = easyCommand;
                            easyCommand = 0;
                        }

                        int systemFlag0 = 0;
                        int systemFlag1 = 0;
                        preText = "";
                        preText1 = "";
                        if (para0 != -1) {//fpgaId
                            preText = "ctr1";
                            preText1 = "sub1";
                            //======
                            ibuf = (int) GB.paraSetMap.get("emulate");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 0;
                            //======
                            ibuf = (int) GB.paraSetMap.get("ctr1Remote");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 2;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("ctr2Remote");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 3;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("mastPulseSource");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 4;
                            ibuf = (int) GB.paraSetMap.get("sub1PulseSource");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 5;
                            ibuf = (int) GB.paraSetMap.get("ctr1PulseSource");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 6;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("ctr1BatShort");//戰備短路 0:關閉 1:開啟
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 7;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("ctr1TxLoad");//輸出裝置 0:假負載 1:天線
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 8;

                            ibuf = (int) GB.paraSetMap.get("sp4tCnt");//輸出裝置 0:假負載 1:天線
                            ibuf &= 7;
                            systemFlag0 |= ibuf << 9;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("mastToSub1CommType");
                            ibuf &= 3;
                            systemFlag0 |= ibuf << 13;
                            ibuf = (int) GB.paraSetMap.get("mastToSub2CommType");
                            ibuf &= 3;
                            systemFlag0 |= ibuf << 15;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("sub1ChCommSet");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 17;
                            ibuf = (int) GB.paraSetMap.get("sub2ChCommSet");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 18;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("sub1CommType");
                            ibuf &= 3;
                            systemFlag0 |= ibuf << 19;
                            ibuf = (int) GB.paraSetMap.get("sub2CommType");
                            ibuf &= 3;
                            systemFlag0 |= ibuf << 21;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("sub1ChSyncType");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 23;
                            ibuf = (int) GB.paraSetMap.get("sub2ChSyncType");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 24;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("mastToSub1SpeechEnable");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 25;
                            ibuf = (int) GB.paraSetMap.get("mastToSub2SpeechEnable");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 26;

                            ibuf = (int) GB.paraSetMap.get("wgProtectFlag");
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 27;

                            //=============================================
                            int sspaPowerV32OnDly = (int) GB.paraSetMap.get(preText + "SspaPowerV32OnDly");//32V 延遲啟動時間 unit 0.1s 8bit
                            int sspaPowerV50OffDly = (int) GB.paraSetMap.get(preText + "SspaPowerV50OffDly");//32V 延遲關閉時間 unit 0.1s 8bit
                            int attenuator = (int) GB.paraSetMap.get(preText + "Attenuator");//衰減器   
                            //=============================================
                            JSONArray jarr = (JSONArray) GB.paraSetMap.get(preText + "SspaPowerExistA");
                            byte[] sspaPowerExistA = new byte[]{0, 0, 0, 0, 0};
                            for (int i = 0; i < 36; i++) {
                                ibuf = (int) jarr.get(i);
                                if (ibuf == 0) {
                                    continue;
                                }
                                sspaPowerExistA[(int) (i / 8)] |= 1 << (i % 8);
                            }
                            //=============================================
                            jarr = (JSONArray) GB.paraSetMap.get(preText + "SspaModuleExistA");
                            byte[] sspaModuleExistA = new byte[]{0, 0, 0, 0, 0};
                            for (int i = 0; i < 36; i++) {
                                ibuf = (int) jarr.get(i);
                                if (ibuf == 0) {
                                    continue;
                                }
                                sspaModuleExistA[(int) (i / 8)] |= 1 << (i % 8);
                            }
                            //=====================================
                            int preTrigTime = (int) GB.paraSetMap.get("preTrigTime");
                            int preRfOutTime = (int) GB.paraSetMap.get("preRfOutTime");
                            int afterTrigTime = (int) GB.paraSetMap.get("preRfOutTime");
                            //=====================================
                            int commTestPacks = (int) GB.paraSetMap.get("commTestPacks");
                            int vgTimeDelay = (int) GB.paraSetMap.get("vgTimeDelay");
                            int chTimeFineTune = (int) GB.paraSetMap.get(preText1 + "ChTimeFineTune");
                            int chFiberDelay = (int) GB.paraSetMap.get(preText1 + "ChFiberDelay");
                            int chRfDelay = (int) GB.paraSetMap.get(preText1 + "ChRfDelay");
                            int sub1ChRfTxCh = (int) GB.paraSetMap.get("sub1" + "ChRfTxCh");
                            int sub2ChRfTxCh = (int) GB.paraSetMap.get("sub2" + "ChRfTxCh");
                            int sub1ChRfRxCh = (int) GB.paraSetMap.get("sub1" + "ChRfRxCh");
                            int sub2ChRfRxCh = (int) GB.paraSetMap.get("sub2" + "ChRfRxCh");
                            int laGroupCh = (int) GB.paraSetMap.get("laGroupCh");
                            int subChDelay = (int) GB.paraSetMap.get("subChDelay");
                            int ctrChDelay = (int) GB.paraSetMap.get("ctrChDelay");
                            int drvChDelay = (int) GB.paraSetMap.get("drvChDelay");
                            int meterChDelay = (int) GB.paraSetMap.get("meterChDelay");

                            //=====================================
                            int pulseGenCh = (int) GB.paraSetMap.get("localPulseGenCh");//pulseGenCh   
                            if (uart0.txAltPackCnt >= 32) {
                                uart0.txAltPackCnt = 0;
                            }
                            jarr = (JSONArray) GB.paraSetMap.get("localPulseGenParas");
                            String str;
                            try {
                                str = (String) jarr.get(uart0.txAltPackCnt);
                            } catch (Exception ex) {
                                str = "0 0 1.0 3.0 1";
                            }
                            String[] strA = str.split(" ");
                            int widthCh = Lib.str2int(strA[1], 0);
                            //=====================================
                            jarr = (JSONArray) GB.paraSetMap.get("localPulseWidthParas");
                            String strw = "10";
                            try {
                                strw = (String) jarr.get(widthCh);
                            } catch (Exception ex) {
                            }
                            int pulseWidth = Math.round((Lib.str2float(strw, 10) * 10));//16bit
                            //=====================================
                            int freq = Math.round((Lib.str2float(strA[3], 3) * 100)) - 290;//8bit
                            int trigTimes = Lib.str2int(strA[4], 1);//8 bit
                            int dutyReg = Math.round((Lib.str2float(strA[2], 1) * 10));//16bit
                            int pri = Math.round(pulseWidth * 1000 / dutyReg);
                            pri &= 0x00ffffff;
                            ibuf = Lib.str2int(strA[0], 0) & 1;//enable_f
                            pri |= ibuf << 24;
                            //===============================
                            /*
                            systemFlag0 = 0x12345678;
                            systemFlag1 = 0x12345678;
                            sspaPowerV32OnDly = 0x56;
                            sspaPowerV50OffDly = 0x78;
                            attenuator = 0x9a;

                            sspaPowerExistA[0] = (byte) 0x01;
                            sspaPowerExistA[1] = (byte) 0x23;
                            sspaPowerExistA[2] = (byte) 0x45;
                            sspaPowerExistA[3] = (byte) 0x67;
                            sspaPowerExistA[4] = (byte) 0x89;

                            sspaModuleExistA[0] = (byte) 0x01;
                            sspaModuleExistA[1] = (byte) 0x23;
                            sspaModuleExistA[2] = (byte) 0x45;
                            sspaModuleExistA[3] = (byte) 0x67;
                            sspaModuleExistA[4] = (byte) 0x89;

                            pulseGenCh = 255;
                            
                            dutyReg = uart0.txAltPackCnt;
                            pulseWidth = uart0.txAltPackCnt * 256 + 1;
                            freq = uart0.txAltPackCnt;
                            trigTimes = uart0.txAltPackCnt;
                             */
                            //============================================
                            ByteLoad lb = new ByteLoad(uart0.txBuffer);
                            lb.wIntInt(systemFlag0);
                            lb.wIntInt(systemFlag1);
                            lb.wByteInt(sspaPowerV32OnDly);
                            lb.wByteInt(sspaPowerV50OffDly);
                            lb.wByteInt(attenuator);
                            //=========================================
                            for (int i = 0; i < 5; i++) {
                                lb.wByteInt(sspaPowerExistA[i]);
                            }
                            for (int i = 0; i < 5; i++) {
                                lb.wByteInt(sspaModuleExistA[i]);
                            }
                            //=========================================
                            lb.wShortInt(preTrigTime);
                            lb.wByteInt(preRfOutTime);
                            lb.wByteInt(afterTrigTime);
                            //=========================================
                            lb.wShortInt(commTestPacks);
                            lb.wShortInt(vgTimeDelay);
                            lb.wShortInt(chTimeFineTune);
                            lb.wShortInt(chFiberDelay);
                            lb.wShortInt(chRfDelay);
                            lb.wByteInt(sub1ChRfTxCh);
                            lb.wByteInt(sub2ChRfTxCh);
                            lb.wByteInt(sub1ChRfRxCh);
                            lb.wByteInt(sub2ChRfRxCh);
                            //==========================================
                            lb.wByteInt(laGroupCh);
                            lb.wByteInt(ctrChDelay);
                            lb.wByteInt(subChDelay);
                            lb.wByteInt(drvChDelay);
                            lb.wByteInt(meterChDelay);
                            //=========================================
                            lb.wByteInt(0xab);
                            if (uart0.txAltPackCnt >= 32) {
                                uart0.txAltPackCnt = 0;
                            }
                            lb.wByteInt(uart0.txAltPackCnt);
                            uart0.txAltPackCnt++;
                            lb.wIntInt(pri);
                            lb.wShortInt(pulseWidth);   //unit 0.1us
                            lb.wByteInt(freq);
                            lb.wByteInt(trigTimes);     //

                            lb.wByteInt(0xcd);
                            uart0.txBufferLen = lb.inx;
                        }

                        uart0.encSend();
                        return null;
                    }
                } catch (Exception ex) {

                }
                //===============================================================
                //uart0.encSend(new byte[]{0x23, 0x02, 0x00, 0x02, 0x00, 0x00, 0x01}, 7);

                return null;
            }
        });
        String errStr = uart0.open();
        return errStr;
    }

    public void txUartTest() {
        uart0.txDeviceId = 0x1234;
        uart0.txSerialId = 0x5678;
        uart0.txGroupId = 0xac00;
        uart0.txCmd = 0xabcd;
        uart0.txPara0 = 0x0001;  //fpgaId
        uart0.txPara1 = 0x0002;  //serialCnt
        uart0.txPara2 = 0x0003; //easy command 
        uart0.txPara3 = 0x0004; //eaay command paras
        uart0.txBufferLen = 0;
        uart0.encSend();

    }

    public void socketServerReturn() {
        byte[] sockUartData_buf = new byte[22];
        int inx = 0;
        sockUartData_buf[inx++] = (byte) ((myDeviceId) & 255);
        sockUartData_buf[inx++] = (byte) ((myDeviceId >> 8) & 255);
        sockUartData_buf[inx++] = (byte) ((mySerialId) & 255);
        sockUartData_buf[inx++] = (byte) ((mySerialId >> 8) & 255);
        int groupFlag = 0xAB00;
        sockUartData_buf[inx++] = (byte) ((groupFlag) & 255);
        sockUartData_buf[inx++] = (byte) ((groupFlag >> 8) & 255);
        int payLoadLen = 14;
        sockUartData_buf[inx++] = (byte) ((payLoadLen) & 255);
        sockUartData_buf[inx++] = (byte) ((payLoadLen >> 8) & 255);
        int cmdInx = 0x1000;
        sockUartData_buf[inx++] = (byte) ((cmdInx) & 255);
        sockUartData_buf[inx++] = (byte) ((cmdInx >> 8) & 255);

        sockUartData_buf[inx++] = (byte) ((GB.realIp[0]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[1]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[2]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[3]) & 255);

        int sockUartData_len = inx;

        MyStm stm = ioComm.serverSocket.stm;
        int stx_index = 0;
        stm.tbuf[stx_index++] = (byte) ((devicePcioId) & 255);
        stm.tbuf[stx_index++] = (byte) ((devicePcioId >> 8) & 255);
        stm.tbuf[stx_index++] = (byte) (255);
        stm.tbuf[stx_index++] = (byte) (255);

        stm.tbuf[stx_index++] = (byte) (0x10);//uart0
        stm.tbuf[stx_index++] = (byte) (0x00);//flag
        stm.tbuf[stx_index++] = (byte) (sockUartData_len & 255);//len low byte
        stm.tbuf[stx_index++] = (byte) ((sockUartData_len >> 8) & 255);//len high byte
        for (int i = 0; i < sockUartData_len; i++) {
            stm.tbuf[stx_index++] = sockUartData_buf[i];
        }
        stm.tbuf_byte = stx_index;
        ioComm.serverSocket.txReturn();
    }

    public String gnRxPrg(String name, byte[] bts, int len) {
        ConsoleMain cla = this;
        int inx = 0;
        int inxLim = len;
        int packageId = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
        int packageSerialId = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
        int packageGroupId = (bts[inx + 4] & 255);
        int packageFlags = (bts[inx + 5] & 255);
        int packageLen = (bts[inx + 7] & 255) * 256 + (bts[inx + 6] & 255);
        inx += 8;
        if (packageId == 0x2303 && packageGroupId == 0x10) {//from Pcio uart11
            int deviceId = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
            int deviceSerialId = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
            inx += 4;
            if (deviceId == myDeviceId && deviceSerialId == 0x00) {//from slot device
                while (inx + 4 < inxLim) {
                    int groupFlag = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
                    int dataLen = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
                    int ix = inx + 4;
                    if (groupFlag == 0xAB00) {//dataBeginId
                        int cmdInx = (bts[ix + 1] & 255) * 256 + (bts[ix + 0] & 255);
                        ix += 2;
                        if (cmdInx == 0x1000) {
                        }
                        ix += 8;
                    }
                    inx += dataLen + 4;
                }
            }
        }
        return null;
    }

    public String cmdPrg(String cmdstr) {
        String errStr = null;
        String content = null;
        if (cmdstr.equals("exit")) {
            System.exit(0);
            return errStr;
        }
        String[] strCmdA = cmdstr.split(" ");


        if (strCmdA[0].equals("changeIp")) {

            //String winCmds="netsh interface ip set address name=乙太網路 source=static addr="+ipStr;
            //String winCmds = "netsh interface ip set address name=區域連線 source=static addr=" + strCmdA[1];
            System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2]);
            if (GB.os_inx == 0) {
                String winCmds = "netsh interface ip set address name=" + strCmdA[1] + " source=static addr=" + strCmdA[2];
                Process pp;
                try {
                } catch (Exception ex) {
                }
            } else {
                String cmdStr = "sudo /usr/sbin/ifconfig " + strCmdA[1] + " " + strCmdA[2] + " netmask " + GB.maskStr;
                System.out.print("\n" + cmdStr);
                Lib.wrInterfaces(strCmdA[1], strCmdA[2], GB.maskStr, GB.gatewayStr);
                if (Lib.exe(cmdStr) == 0) {
                    System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2] + " OK.");
                } else {
                    System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2] + " Error !!! ");
                }
            }
            return errStr;
        }
        return errStr;
    }

    public void addTask(String[] strCmdA, int retryAmt, int retryDly) {
        CmdTask task1 = new CmdTask(strCmdA[0]);
        for (int i = 1; i < strCmdA.length; i++) {
            task1.paras[i - 1] = strCmdA[i];
        }
        task1.retryAmt = retryAmt;
        task1.retryDly = retryDly;
        cexe.addMap(task1);
    }

    public String cmdFunc(String cmdstr) {
        final ConsoleMain cla = this;
        String errStr = null;
        String cmd = "";
        if (cmdstr.equals(":")) {
            cmdstr = preCmdStr;
        }
        preCmdStr = cmdstr;
        if (cmdstr.equals("exit")) {
            System.exit(0);
            return errStr;
        }
        
        if (cmdstr.equals("simulateKeyExit")) {
            try {
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_F4);
                robot.keyRelease(KeyEvent.VK_F4);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            } catch (AWTException e) {
                e.printStackTrace();
            }

        }
        

        if (cmdstr.equals("listComPort")) {
            String[] list = Uart.listComPort();
            String str = "ComPort: ";
            for (int i = 0; i < list.length; i++) {
                if (i != 0) {
                    str += ", ";
                }
                str += list[i];
            }
            System.out.println(str);
            return errStr;
        }

        if (cmdstr.equals("openComPort")) {
            errStr = openUart0();
            return errStr;
        }
        if (cmdstr.equals("closeComPort")) {
            uart0.close();
            return errStr;
        }
        if (cmdstr.equals("sendComTest")) {
            uart0.send("uart0 tx test");
            return errStr;
        }

        if (cmdstr.equals("test1")) {
            try {
                Path file = Paths.get(GB.paraSetPath);
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class
                );
                System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
            } catch (Exception ex) {

            }
            return errStr;
        }
        String[] strCmdA = cmdstr.split(" ");

        if (strCmdA[0].equals("txsskui")) {
            if (cmdstr.length() > 8) {
            }
            return errStr;

        }

        if (strCmdA[0].equals("txFileToSocket")) {
            return errStr;
        }

        //=================================================
        cmd = "testResponse";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        if (cmdstr.equals("bypassSystemSecurity")) {
            return errStr;
        }
        if (cmdstr.equals("clearSystemSecurity")) {
            return errStr;
        }

        return "Command Not Found !!!";

    }

}

class ConsoleMainTm1 extends TimerTask {

    ConsoleMain cla;
    String preParaSetTime = "";

    ConsoleMainTm1(ConsoleMain owner) {
        cla = owner;
    }

    @Override
    public void run() {
        try {
            //===============================

            if (GB.emulate == 2) {
                cla.syncData.systemStatus0 &= 0xfffffffc;
                cla.syncData.systemStatus0 |= 0x00000002;
            }

            cla.easyCommandTime++;
            if (cla.easyCommandTime == 50) {
                cla.easyCommand = 0;

            }

            for (int i = 0; i < 36; i++) {
                cla.drvDataClrBuf[i]++;
                if (cla.drvDataClrBuf[i] > 50) {
                    cla.syncData.sspaPowerStatusAA[i] &= 0xfe;
                }
            }

            cla.testUartTime++;
            if (cla.testUartTime >= 5) {
                cla.testUartTime = 0;
                //cla.uart0.encSend(new byte[]{0x23, 0x02, 0x00, 0x02, 0x00, 0x00, 0x01}, 7);
                //System.out.println("txTest");

            }
            cla.connectFpgaCnt++;
            if (cla.connectFpgaCnt > 100) {
                cla.syncData.systemStatus0 &= 0xfffffffc;
                cla.syncData.pulseFormHighPeriod = 0;
                cla.syncData.pulseFormLowPeriod = 0;
                cla.syncData.pulseFormAddBufA0[cla.syncData.pulseFormAddBufA0Inx1 & 255] = 20 * 1000 * 160 * 2;
                cla.syncData.pulseFormAddBufA0Inx1++;

            }

            //===============================
            Path file = Paths.get(GB.paraSetPath);
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            String nowParaSetTime = attr.lastModifiedTime().toString();
            if (!preParaSetTime.equals(nowParaSetTime)) {
                preParaSetTime = nowParaSetTime;
                System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
                String content = Lib.readFile("paraSet.json");
                GB.paraSetMap.clear();
                JSONObject jsPara = new JSONObject(content);
                Iterator<String> it = jsPara.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    GB.paraSetMap.put(key, jsPara.get(key));
                }
                cla.appId = (int) GB.paraSetMap.get("appId");
                GB.emulate = (int) GB.paraSetMap.get("emulate");
                if (GB.webSocketAddr == null) {
                    GB.webSocketAddr = (String) GB.paraSetMap.get("webSocketAddr");
                    KvWebSocketServer.serverStart();
                }

            }

            cla.cexe.exeTaskMap();
        } catch (Exception ex) {
            //System.out.println(ex.toString());
        }
    }

}

class ConsoleMainCmdExe {

    ConsoleMain cla;
    Map<String, CmdTask> taskMap;

    ConsoleMainCmdExe(ConsoleMain owner, Map<String, CmdTask> _taskMap) {
        cla = owner;
        taskMap = _taskMap;
    }

    public void exeTaskMap() {
        try {
            Set<String> iss = taskMap.keySet();
            Object[] objA = iss.toArray();
            for (int i = 0; i < objA.length; i++) {
                if (objA[i] == null) {
                    continue;
                }
                String key = (String) objA[i];
                CmdTask task1 = taskMap.get(key);
                if (task1 == null) {
                    continue;
                }
                exeTask(task1);

            }

            /*
            for (String key : iss) {
                CmdTask task1 = taskMap.get(key);
                if (task1 == null) {
                    continue;
                }
                exeTask(task1);
            }
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public CmdTask getMap(String name) {
        CmdTask task = taskMap.get(name);
        return task;
    }

    public void addMap(CmdTask task) {
        taskMap.put(task.name, task);
    }

    public int taskEnd(CmdTask task) {
        task.stepInx = 0;
        task.stepTim = 0;
        task.retryTim = 0;
        task.retryCnt += 1;
        if (task.retryAmt > 0) {
            if (task.retryCnt >= task.retryAmt) {
                taskMap.remove(task.name);
                return 1;
            }
        }
        return 0;
    }

    //if slotCnt < 0 commsnd slot all
    public int exeTask(CmdTask task) {
        if (task.retryTim < task.retryDly) {
            task.retryTim++;
            return 0;
        }
        switch (task.name) {
            case "reNewExtensions":
            case "reNewParaSet":
            case "upLoadFile":
            case "readFile":
            case "getExRecordNames":
            case "getRecordFile":
            case "testResponse":
            case "getSlotInf":
            case "startAsterisk":
            case "stopAsterisk":
            default:
                return 0;
        }
    }
}

class SyncData {

    /*
	 array 0:mast, 1:sub1, 2:sub2, 3:ctr1, 4:ctr2, 5:drv1a, 6:drv1b, 7:drv2a, 8:drv2b
	 *** slotId[3:0] ==>
	 	 "none 			id=0;
	 	 "ＩＰＣ控制模組",     	id=1;
	 	 "ＦＰＧＡ控制模組",    id=2;
	 	 "ＩＯ控制模組",       id=3;
	 	 "邏輯分析模組",       id=4;
	 	 "光纖傳輸模組",     	id=5;
	 	 "ＲＦ傳輸模組	",     	id=6;
	 	 "語音通信模組",   	id=7;
	 	 "SSPA驅動模組",   	id=8;
	  *** slotSerNo		7:4
	  *** slotStatus	9:8 ==> 0:none, 1:ready, 2:error 3:warn up
          *** slotTestStatus 11:10 ==> 0:none, 1:PreTest, 2:testing;
     */
    short[] slotDataAA = new short[12];

    /*=================================================
     mast mainStatus[1:0] 		==> 0:none, 1:warn up, 2:ready, 3:error
     sub1 mainStatus[3:2] 		==> 0:none, 1:warn up, 2:ready, 3:error
     sub2 mainStatus[5:4] 		==> 0:none, 1:warn up, 2:ready, 3:error
     ctr1 mainStatus[7:6] 		==> 0:none, 1:warn up, 2:ready, 3:error
     ctr2 mainStatus[9:8] 		==> 0:none, 1:warn up, 2:ready, 3:error
     drv1a mainStatus[11:10] 	==> 0:none, 1:warn up, 2:ready, 3:error
     drv1b mainStatus[13:12] 	==> 0:none, 1:warn up, 2:ready, 3:error
     drv2a mainStatus[15:14] 	==> 0:none, 1:warn up, 2:ready, 3:error
     drv2b mainStatus[17:16] 	==> 0:none, 1:warn up, 2:ready, 3:error
     ctr1Meter mainStatus[19:18] 	==> 0:none, 1:warn up, 2:ready, 3:error
     ctr2Meter mainStatus[21:20] 	==> 0:none, 1:warn up, 2:ready, 3:error
     //===
     ctr1 rfPulse detect flag[22]       ==> 0:none  1:OK
     ctr1 電源啟動[23] 			==> 0:停止 1:啟動
     ctr1 SSPA致能[24] 			==> 0:停止 1:啟動
     ctr1 本地脈波啟動[25] 		==> 0:停止 1:啟動
     ctr1 緊急停止[26] 			==> 0:備便 1:停止
     //===
     ctr2 rfPulse detect flag[27] ==> 0:none  1:OK
     ctr2 電源啟動[28] 			==> 0:停止 1:啟動
     ctr2 SSPA致能[29] 			==> 0:停止 1:啟動
     ctr2 本地脈波啟動[30] 		==> 0:停止 1:啟動
     ctr2 緊急停止[31] 			==> 0:備便 1:停止
     */
    int systemStatus0;
    /*=================================================
    sub1 光纖連線狀態[0]	==> 0:未連線, 1:未連線
    sub1 RF連線狀態[1]          ==> 0:未連線, 1:未連線
    sub2 光纖連線狀態[2] 	==> 0:未連線, 1:未連線
    sub2 RF連線狀態[3]          ==> 0:未連線, 1:未連線
    ctr1 遠端遙控[4]      ==> 0:關閉, 1:開啟
    ctr2 遠端遙控[5]      ==> 0:關閉, 1:開啟
    mast spPulseExist[6]	==  0:none 1:exist

    
     */
    int systemStatus1;

    /* enviStatus every item is 1 bit
     value 0:none, 1:error
     airFlow left
     airFlow middle
     airFlow right
     waterFlow 1
     waterFlow 2
     waterFlow 3
     waterFlow 4
     waterFlow 5
     waterFlow 6
     waterFlow temperature
     */
    int[] enviStatusA = new int[]{0, 0};
    /*
     0:input rf power
     1:
     2:pre amp output rf power
     3:driver amp output rf power
     4:cw output rf power
     5:ccw output rf power
     */
    short[] meterStatusAA = new short[6];
    //=============================================
    //0 connectFlag, 1 faultLed, 2:v50enLed, 3:v32enLed, 4:v50v, 5:v50i, 6:v50t, 7:v32v, 8:v32i, 9:v32t
    byte[] sspaPowerStatusAA = new byte[36];
    short[] sspaPowerV50vAA = new short[36];
    short[] sspaPowerV50iAA = new short[36];
    short[] sspaPowerV50tAA = new short[36];
    short[] sspaPowerV32vAA = new short[36];
    short[] sspaPowerV32iAA = new short[36];
    short[] sspaPowerV32tAA = new short[36];
    //=============================================
    /*
     0:connect, 1:致能, 2 保護觸發, 3:工作比過高, 4:脈寬過高, 5:溫度過高, 6:反射過高, 
     */
    byte[] sspaModuleStatusAA = new byte[36];
    short[] sspaModuleRfOutAA = new short[36];
    short[] sspaModuleTemprAA = new short[36];
    //=============================================

    byte[][] gpsDataAA = new byte[3][16];
    byte[] gngga0 = new byte[64];
    byte[] gngga1 = new byte[64];
    byte[] gngga2 = new byte[64];

    short[] adjTimeOf1588A = new short[2];
    short[] commPackageCntA = new short[2];
    short[] commOkRateA = new short[2];
    short[] rfRxPowerA = new short[4];//mast rx1,mast rx1,sub1 rx sub2 rx

    int[] pulseWaveA = new int[256];
    int pulseWaveInx = 0;

    int[] viewDatas = new int[256];
    //ArrayList<int> list = new ArrayList<int>();
    int pulseFormAddBufA0Len = 0;
    int pulseFormAddBufA0Inx0 = 0;
    int pulseFormAddBufA0Inx1 = 0;
    int[] pulseFormAddBufA0 = new int[256];
    int pulseFormLowPeriod = 0;
    int pulseFormHighPeriod = 0;
    byte pulseFormFreq = 0;

    SyncData() {
    }

}

class ByteLook {

    byte[] bta;
    int inx = 0;

    ByteLook(byte[] arr) {
        bta = arr;
    }

    int getByteInt(int ii) {
        return bta[ii] & 255;
    }

    byte lookByte() {
        return bta[inx++];
    }

    int lookByteInt() {
        return bta[inx++] & 255;
    }

    short lookShort() {
        int ibuf = (bta[inx++]) & 255;
        ibuf += (bta[inx++] & 255) * 256;
        return (short) ibuf;
    }

    int lookShortInt() {
        int ibuf = (bta[inx++]) & 255;
        ibuf += (bta[inx++] & 255) * 256;
        return ibuf;
    }

    int lookInt() {
        int ibuf = (bta[inx++]) & 255;
        ibuf += (bta[inx++] & 255) * 256;
        ibuf += (bta[inx++] & 255) * 65536;
        ibuf += (bta[inx++] & 255) * 16777216;
        return ibuf;
    }

}

class ByteLoad {

    byte[] bta;
    int inx = 0;

    ByteLoad(byte[] arr) {
        bta = arr;
    }

    void wByteByte(byte data) {
        bta[inx++] = data;
    }

    void wByteInt(int data) {
        bta[inx++] = (byte) (data & 255);
    }

    void wShortInt(int data) {
        bta[inx++] = (byte) (data & 255);
        bta[inx++] = (byte) ((data >> 8) & 255);
    }

    void wIntInt(int data) {
        bta[inx++] = (byte) (data & 255);
        bta[inx++] = (byte) ((data >> 8) & 255);
        bta[inx++] = (byte) ((data >> 16) & 255);
        bta[inx++] = (byte) ((data >> 24) & 255);
    }

}
