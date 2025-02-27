package base3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
            if (act.equals("openAllSspaPower")) {
                if (GB.emulate == 1) {
                }
                outJson.put("status", "ok");
                return outJson;
            }

            if (act.equals("closeAllSspaPower")) {
                if (GB.emulate == 1) {
                }
                outJson.put("status", "ok");
                return outJson;
            }

            if (act.equals("openAllSspaModule")) {
                outJson.put("status", "ok");
                return outJson;
            }

            if (act.equals("closeAllSspaModule")) {
                outJson.put("status", "ok");
                return outJson;
            }

            if (act.equals("localPulseOn")) {
                outJson.put("status", "ok");
                return outJson;
            }

            if (act.equals("localPulseOff")) {
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals("emergencyRelease")) {
                outJson.put("status", "ok");
                return outJson;
            }
            if (act.equals("emergencyStop")) {
                outJson.put("status", "ok");
                return outJson;
            }

        } catch (Exception ex) {

        }
        return outJson;

    }

    public void transSyncData(JSONObject outJson) {
        try {
            if (appId == 3 || appId == 4) {
                KvJson kj = new KvJson();
                kj.jStart();
                kj.jadd("slotIdA", syncData.slotIdA);
                kj.jadd("slotStatusA", syncData.slotStatusA);
                kj.jadd("slotTestStatusA", syncData.slotTestStatusA);
                kj.jadd("systemStatus0", syncData.systemStatus0);
                kj.jadd("envStatusA#"+(appId-3), syncData.enviStatusA[appId-3]);
                kj.jadd("meterStatusA#"+(appId-3), syncData.meterStatusAA[appId-3]);
                kj.jadd("sspaPowerStatusA#"+(appId-3), syncData.sspaPowerStatusAA[appId-3]);
                kj.jadd("sspaPowerV50vA#"+(appId-3), syncData.sspaPowerV50vAA[appId-3]);
                kj.jadd("sspaPowerV50iA#"+(appId-3), syncData.sspaPowerV50iAA[appId-3]);
                kj.jadd("sspaPowerV50tA#"+(appId-3), syncData.sspaPowerV50tAA[appId-3]);
                kj.jadd("sspaPowerV32vA#"+(appId-3), syncData.sspaPowerV32vAA[appId-3]);
                kj.jadd("sspaPowerV32iA#"+(appId-3), syncData.sspaPowerV32iAA[appId-3]);
                kj.jadd("sspaPowerV32tA#"+(appId-3), syncData.sspaPowerV32tAA[appId-3]);
                kj.jadd("sspaModuleStatusA#"+(appId-3), syncData.sspaModuleStatusAA[appId-3]);
                kj.jadd("sspaModuleRfOutA#"+(appId-3), syncData.sspaModuleRfOutAA[appId-3]);
                kj.jadd("sspaModuleRfTemprA#"+(appId-3), syncData.sspaModuleTemprAA[appId-3]);
                kj.jEnd();
                JSONObject syncJson = new JSONObject(kj.jstr);
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
        KvWebSocketServer.serverStart();
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
        uart0.boudrate = (int) GB.paraSetMap.get("uart0Boudrate");
        uart0.parity = "None";
        uart0.stopBit = 1;
        uart0.dataBit = 8;
        uart0.txEncMode = 1;
        uart0.rxEncMode = 1;
        uart0.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                int deviceId = (bytes[0] & 255) + (bytes[1] & 255) * 256;
                int serialId = (bytes[2] & 255) + (bytes[3] & 255) * 256;
                int groupId = (bytes[4] & 255) + (bytes[5] & 255) * 256;
                int groupLen = (bytes[6] & 255) + (bytes[7] & 255) * 256;
                int cmd = (bytes[8] & 255) + (bytes[9] & 255) * 256;
                int para0 = (bytes[10] & 255) + (bytes[11] & 255) * 256;
                int para1 = (bytes[12] & 255) + (bytes[13] & 255) * 256;
                int para2 = (bytes[14] & 255) + (bytes[15] & 255) * 256;
                int para3 = (bytes[16] & 255) + (bytes[17] & 255) * 256;
                if (deviceId != 25010 || serialId != 0x0000) {
                    return null;
                }
                int inx=18;
                int ibuf=0;
                if(cmd == 0x1000){
                    for(int i=0;i<12;i++){
                        ibuf=bytes[inx++] & 255;
                        syncData.slotIdA[i]=ibuf&0x0f;
                        syncData.slotStatusA[i]=(ibuf>>4)&0x03;
                        syncData.slotTestStatusA[i]=(ibuf>>6)&0x03;
                    }
                        
                    
                    
                }
                    
                    
                
                uart0.rxSerialCnt++;
                uart0.rxSerialCnt &= 0xffff;
                if (uart0.rxSerialCnt != para1) {
                    uart0.rxErrCnt++;
                }
                uart0.rxSerialCnt = para1;
                uart0.rxPackageCnt++;
                if ((uart0.rxPackageCnt % 100) == 0) {
                    System.out.print(" uart0Rx-" + uart0.rxErrCnt);
                    if ((uart0.rxPackageCnt % 1000) == 0) {
                        System.out.print("\n");
                    }
                }
                //===============================================================
                String preText;
                try {
                    if (cmd == 0x1000) {//tick
                        uart0.txDeviceId = deviceId;
                        uart0.txSerialId = serialId;
                        uart0.txGroupId = 0xac00;
                        uart0.txCmd = cmd;
                        uart0.txPara0 = para0;  //fpgaId
                        uart0.txPara1 = para1;  //serialCnt
                        uart0.txPara2 = 0x0000;
                        uart0.txPara3 = 0x0000;
                        uart0.txBufferLen = 0;
                        int systemFlag0 = 0;
                        preText = "";
                        if (para0 == 3 || para0 == 4) {//fpgaId
                            if (para0 == 3) {
                                preText = "ctr1";
                            }
                            if (para0 == 4) {
                                preText = "ctr2";
                            }
                            //======
                            ibuf = (int) GB.paraSetMap.get(preText + "Remote");//遠端遙控 0:disable 1:enable
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 0;
                            //=======
                            ibuf = (int) GB.paraSetMap.get(preText + "PulseSource");//脈波來源 0:同步脈波 1:本機脈波
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 1;
                            //=======
                            ibuf = (int) GB.paraSetMap.get(preText + "BatShort");//戰備短路 0:關閉 1:開啟
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 2;
                            //=======
                            ibuf = (int) GB.paraSetMap.get(preText + "TxLoad");//輸出裝置 0:假負載 1:天線
                            ibuf &= 1;
                            systemFlag0 |= ibuf << 3;
                            //=======
                            ibuf = (int) GB.paraSetMap.get("emulate");
                            ibuf &=3;
                            systemFlag0 |= ibuf << 4;
                            //=======
                            
                            
                            
                            int sspaPowerV32OnDly = (int) GB.paraSetMap.get(preText + "SspaPowerV32OnDly");//32V 延遲啟動時間 unit 0.1s 8bit
                            int sspaPowerV32OffDly = (int) GB.paraSetMap.get(preText + "SspaPowerV32OffDly");//32V 延遲關閉時間 unit 0.1s 8bit
                            int attenuator = (int) GB.paraSetMap.get(preText + "Attenuator");//衰減器   
                            //=============================================
                             JSONArray jarr = (JSONArray) GB.paraSetMap.get(preText + "SspaPowerExistA");
                            byte[] sspaPowerExistA= new byte[]{0,0,0,0,0};
                            for (int i = 0; i < 36; i++) {
                                ibuf=(int)jarr.get(i);
                                if(ibuf==0)
                                    continue;
                                sspaPowerExistA[(int)(i/8)]|=1<<(i%8);
                            }
                            //=============================================
                             jarr = (JSONArray) GB.paraSetMap.get(preText + "SspaModuleExistA");
                            byte[] sspaModuleExistA= new byte[]{0,0,0,0,0};
                            for (int i = 0; i < 36; i++) {
                                ibuf=(int)jarr.get(i);
                                if(ibuf==0)
                                    continue;
                                sspaModuleExistA[(int)(i/8)]|=1<<(i%8);
                            }
                            
                            
                            
                            //=====================================
                            int pulseGenCh = (int) GB.paraSetMap.get("pulseGenCh");//pulseGenCh   
                            if (uart0.txAltPackCnt >= 32) {
                                uart0.txAltPackCnt = 0;
                            }
                            jarr = (JSONArray) GB.paraSetMap.get("localPulseGenParas");
                            String str;
                            try {
                                str = (String) jarr.get(uart0.txAltPackCnt);
                            } catch (Exception ex) {
                                str = "0 10 1.0 3.0 1";
                            }
                            String[] strA = str.split(" ");
                            int dutyReg = Math.round((Lib.str2float(strA[2], 1) * 10));//16bit
                            ibuf = Lib.str2int(strA[0], 0);
                            ibuf &= 1;
                            dutyReg += ibuf << 12;
                            //=====================================
                            int pulseWidth = Lib.str2int(strA[1], 10);//16 bit
                            int freq = Math.round((Lib.str2float(strA[3], 3) * 10));//8bit
                            int trigTimes = Lib.str2int(strA[1], 1);//8 bit
                            inx = 0;
                            //<<debug
    //===============================
                            systemFlag0=0x12345678;
                            sspaPowerV32OnDly=0x56;
                            sspaPowerV32OffDly=0x78;
                            attenuator=0x9a;
                            
                            sspaPowerExistA[0]=(byte)0x01;
                            sspaPowerExistA[1]=(byte)0x23;
                            sspaPowerExistA[2]=(byte)0x45;
                            sspaPowerExistA[3]=(byte)0x67;
                            sspaPowerExistA[4]=(byte)0x89;
                            
                            sspaModuleExistA[0]=(byte)0x01;
                            sspaModuleExistA[1]=(byte)0x23;
                            sspaModuleExistA[2]=(byte)0x45;
                            sspaModuleExistA[3]=(byte)0x67;
                            sspaModuleExistA[4]=(byte)0x89;
                            
                            pulseGenCh=255;
                            dutyReg=uart0.txAltPackCnt;
                            pulseWidth = uart0.txAltPackCnt*256+1;
                            freq = uart0.txAltPackCnt;
                            trigTimes = uart0.txAltPackCnt;
                            //============================================
                            
                            uart0.txBuffer[inx++] = (byte) ((systemFlag0) & 255);
                            uart0.txBuffer[inx++] = (byte) ((systemFlag0 >> 8) & 255);
                            uart0.txBuffer[inx++] = (byte) ((systemFlag0 >> 16) & 255);
                            uart0.txBuffer[inx++] = (byte) ((systemFlag0 >> 24) & 255);
                            uart0.txBuffer[inx++] = (byte) ((sspaPowerV32OnDly) & 255);
                            uart0.txBuffer[inx++] = (byte) ((sspaPowerV32OffDly) & 255);
                            uart0.txBuffer[inx++] = (byte) ((attenuator) & 255);
                            //=========================================
                            for(int i=0;i<5;i++){
                                uart0.txBuffer[inx++] = (byte) ((sspaPowerExistA[i]) & 255);
                            }
                            for(int i=0;i<5;i++){
                                uart0.txBuffer[inx++] = (byte) ((sspaModuleExistA[i]) & 255);
                            }
                            //=========================================
                            uart0.txBuffer[inx++] = (byte) ((0xab) & 255);
                            uart0.txBuffer[inx++] = (byte) ((pulseGenCh) & 255);
                            uart0.txBuffer[inx++] = (byte) ((uart0.txAltPackCnt) & 255);
                            uart0.txAltPackCnt++;
                            uart0.txBuffer[inx++] = (byte) ((dutyReg) & 255);
                            uart0.txBuffer[inx++] = (byte) ((dutyReg >> 8) & 255);
                            uart0.txBuffer[inx++] = (byte) ((pulseWidth) & 255);
                            uart0.txBuffer[inx++] = (byte) ((pulseWidth >> 8) & 255);
                            uart0.txBuffer[inx++] = (byte) ((freq) & 255);
                            uart0.txBuffer[inx++] = (byte) ((trigTimes) & 255);
                            uart0.txBufferLen = inx;
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
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
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
         "ＩＰＣ控制模組",      id=1; 
         "ＦＰＧＡ控制模組",    id=2; 
         "ＩＯ控制模組",        id=3; 
         "邏輯分析模組",        id=4; 
         "光纖傳輸模組 １",     id=5; 
         "光纖傳輸模組 ２",     id=6; 
         "光纖傳輸模組 ３",     id=7; 
         "光纖傳輸模組 ４",     id=8; 
         "ＲＦ傳輸模組 Ａ",     id=9; 
         "ＲＦ傳輸模組 Ｂ",     id=10; 
         "語音通信模組 Ａ",     id=11; 
         "語音通信模組 Ｂ"      id=12
     */
    //slotId[3:0]
    //slotSerNo[7:4]
    int[] slotIdA = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    // 0:none, 1:ready, 2:error 3:warn up
    int[] slotStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    //0:none, 1:PreTest,2:testing;
    int[] slotTestStatusA = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    //
    
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
     meter mainStatus[19:18] 	==> 0:none, 1:warn up, 2:ready, 3:error
     rfPulse detect flag[21:20] ==> 0:unknow ,1: none  ,2:ok
            電源啟動[22] 					==> 0:停止 1:啟動
     SSPA致能[23] 				==> 0:停止 1:啟動
            本地脈波啟動[24] 				==> 0:停止 1:啟動
            緊急停止[25] 					==> 0:備便 1:停止
     */
    int systemStatus0;
   /* enviStatus every item is 2 bit
     value 0:none, 1:ok, 2:error
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
    int[] enviStatusA=new int[]{0,0};    
    
    short[][] meterStatusAA=new short[2][6];
    //=============================================
    //0 connectFlag, 1 faultLed, 2:v50enLed, 3:v32enLed, 4:v50v, 5:v50i, 6:v50t, 7:v32v, 8:v32i, 9:v32t
    byte[][] sspaPowerStatusAA=new byte[2][36];
    short[][] sspaPowerV50vAA=new short[2][36];
    short[][] sspaPowerV50iAA=new short[2][36];
    short[][] sspaPowerV50tAA=new short[2][36];
    short[][] sspaPowerV32vAA=new short[2][36];
    short[][] sspaPowerV32iAA=new short[2][36];
    short[][] sspaPowerV32tAA=new short[2][36];
    //=============================================
    /*
     0:connect, 1:致能, 2 保護觸發, 3:工作比過高, 4:脈寬過高, 5:溫度過高, 6:反射過高, 7:RF輸出, 8:溫度
     */
    byte[][] sspaModuleStatusAA=new byte[2][36];
    short[][] sspaModuleRfOutAA=new short[2][36];
    short[][] sspaModuleTemprAA=new short[2][36];
    //=============================================
    byte[][] gpaDataAA=new byte[3][16];
    short[] adjTimeOf1588A=new short[2];
    short[] commPackageCntA=new short[2];
    short[] commOkRateA=new short[2];
    short[] rfRxPowerA=new short[4];//mast rx1,mast rx1,sub1 rx sub2 rx
    

    SyncData() {
    }

}
