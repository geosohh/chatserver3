package com.chat3.server.model;

public class WebSocketDataFrame {
    
    private int fin;
    private int rsv1;
    private int rsv2;
    private int rsv3;
    private int opcode;
    private int mask;
    private long payloadLen;
    private byte[] maskingKey;
    
    private String payloadText;
    private byte[] payloadBinary;
    
    public WebSocketDataFrame(){
    }
    
    public WebSocketDataFrame(String payloadData){
        fin = 0x80;
        mask = 0x0;
        opcode = WebSocketOpcode.TEXT;
        payloadText = payloadData;
        payloadLen = payloadData.getBytes().length;
    }
    
    public boolean isFin() {
        return (fin & 0xff) != 0;
    }
    public boolean isRsv1() {
        return (rsv1 & 0xff) != 0;
    }
    public boolean isRsv2() {
        return (rsv2 & 0xff) != 0;
    }
    public boolean isRsv3() {
        return (rsv3 & 0xff) != 0;
    }
    public int getOpcode() {
        return opcode;
    }
    public boolean hasMask() {
        return (mask & 0xff) != 0;
    }
    public long getPayloadLen() {
        return payloadLen;
    }
    public byte[] getMaskingKey() {
        return maskingKey;
    }
    public String getPayloadText() {
        return payloadText;
    }
    public byte[] getPayloadBinary() {
        return payloadBinary;
    }
    
    
    
    public void setFin(int fin) {
        this.fin = fin;
    }
    public void setRsv1(int rsv1) {
        this.rsv1 = rsv1;
    }
    public void setRsv2(int rsv2) {
        this.rsv2 = rsv2;
    }
    public void setRsv3(int rsv3) {
        this.rsv3 = rsv3;
    }
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
    public void setMask(int mask) {
        this.mask = mask;
    }
    public void setPayloadLen(long payloadLen) {
        this.payloadLen = payloadLen;
    }
    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }
    public void setPayloadText(byte[] payloadData) {
        if (hasMask()){
            for (int i=0; i<payloadData.length; i++){
                payloadData[i] = (byte) (payloadData[i] ^ maskingKey[i%4]);
            }
        }
        this.payloadText = new String(payloadData);
    }
    public void setPayloadBinary(byte[] payloadData) {
        if (hasMask()){
            for (int i=0; i<payloadData.length; i++){
                payloadData[i] = (byte) (payloadData[i] ^ maskingKey[i%4]);
            }
        }
        this.payloadBinary = payloadData;
    }
    
    public byte[] createResponseDataFrame(){
        int bytesNeeded = 2;
        long extendedPayloadLen = 0;
        if (payloadLen > 125){
            // payloadData.getBytes().length returns an int,
            // so in practice extendedPayloadLen will never
            // be more than 65535...
            extendedPayloadLen = payloadLen;
            if (extendedPayloadLen <= 65535){ //16 bits
                bytesNeeded += 2;
                payloadLen = 126;
            }
            else{
                bytesNeeded += 8;
                payloadLen = 127;
            }
        }
        bytesNeeded += payloadText.getBytes().length;
        
        byte[] dataFrame = new byte[bytesNeeded];
        dataFrame[0] = (byte)(fin | opcode);
        dataFrame[1] = (byte)((mask & 0xff) | (payloadLen & 0xff));
        if (payloadText.length()>0){
            int currentByte = 2;
            if (extendedPayloadLen != 0){
                if (extendedPayloadLen <= 65535){
                    dataFrame[2] = (byte) (extendedPayloadLen >> 8);
                    dataFrame[3] = (byte) (extendedPayloadLen);
                    currentByte = 4;
                }
                else{
                    dataFrame[2] = (byte) (extendedPayloadLen >> 8*7);
                    dataFrame[3] = (byte) (extendedPayloadLen >> 8*6);
                    dataFrame[4] = (byte) (extendedPayloadLen >> 8*5);
                    dataFrame[5] = (byte) (extendedPayloadLen >> 8*4);
                    dataFrame[6] = (byte) (extendedPayloadLen >> 8*3);
                    dataFrame[7] = (byte) (extendedPayloadLen >> 8*2);
                    dataFrame[8] = (byte) (extendedPayloadLen >> 8);
                    dataFrame[9] = (byte) (extendedPayloadLen);
                    currentByte = 10;
                }
            }
            byte[] payloadBytes = payloadText.getBytes();
            for (int i=0; i<payloadBytes.length; i++){
                dataFrame[currentByte] = payloadBytes[i];
                currentByte++;
            }
        }
        return dataFrame;
    }
}
