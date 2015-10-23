package com.chat3.server.model;

public class WebSocketDataFrameParser {
    
    private WebSocketDataFrame webSocketInput = new WebSocketDataFrame();
    private byte[] extendedPayloadLen = null;
    private byte[] maskingKey = null;
    private byte[] payloadData = null;
    private int originalPayloadLen = 0;
    
    private boolean starting = true;
    private boolean opcodeRead = false;
    private boolean payloadLenRead = false;
    private boolean extendedPayloadLenRead = false;
    private boolean maskingKeyRead = false;
    
    private int extendedPayloadBytesRead = 0;
    private int maskingKeyBytesRead = 0;
    private int payloadBytesRead = 0;
    
    public boolean parse(int byteValue){
        boolean parsingDone = false;
        
        if (starting)
            parseStarting(byteValue);
        
        else if (opcodeRead)
            parseOpcodeRead(byteValue);
        
        else if (payloadLenRead)
            parsePayloadLenRead(byteValue);
        
        else if (extendedPayloadLenRead)
            parseExtendedPayloadLenRead(byteValue);
        
        else if (maskingKeyRead && webSocketInput.getPayloadLen()>0)
            parsingDone = parseMaskingKeyRead(byteValue);

        return !parsingDone;
    }
    
    private void parseStarting(int byteValue){
        webSocketInput.setFin( byteValue & 0x80);
        webSocketInput.setRsv1(byteValue & 0x40);
        webSocketInput.setRsv2(byteValue & 0x20);
        webSocketInput.setRsv3(byteValue & 0x10);
        webSocketInput.setOpcode(byteValue & 0xF);
        starting = false;
        opcodeRead = true;
    }
    
    private void parseOpcodeRead(int byteValue){
        webSocketInput.setMask(byteValue & 0x80);
        webSocketInput.setPayloadLen(byteValue & 0x7F);
        originalPayloadLen = (int)webSocketInput.getPayloadLen();
        opcodeRead = false;
        if (originalPayloadLen<126){
            if (!webSocketInput.hasMask())
                maskingKeyRead = true;
            else
                extendedPayloadLenRead = true;
        }
        else
            payloadLenRead = true;
    }
    
    private void parsePayloadLenRead(int byteValue){
        if (originalPayloadLen==126){
            if (extendedPayloadBytesRead==0){
                extendedPayloadLen = new byte[2]; // 16 bits
                extendedPayloadLen[0] = (byte)byteValue;
                extendedPayloadBytesRead++;
            }
            else{
                extendedPayloadLen[1] = (byte)byteValue;
                webSocketInput.setPayloadLen(
                    ((extendedPayloadLen[0] & 0xff) << 8) | (extendedPayloadLen[1] & 0xff)
                );
                payloadLenRead = false;
                if (!webSocketInput.hasMask())
                    maskingKeyRead = true;
                else
                    extendedPayloadLenRead = true;
            }
        }
        else if (originalPayloadLen==127){
            if (extendedPayloadBytesRead==0)
                extendedPayloadLen = new byte[8]; // 64 bits
            
            extendedPayloadLen[extendedPayloadBytesRead] = (byte)byteValue;
            extendedPayloadBytesRead++;
            
            if (extendedPayloadBytesRead==8){
                webSocketInput.setPayloadLen(
                    ((long)(extendedPayloadLen[0] & 0xff) << 56) | 
                    ((long)(extendedPayloadLen[1] & 0xff) << 48) | 
                    ((long)(extendedPayloadLen[2] & 0xff) << 40) | 
                    ((long)(extendedPayloadLen[3] & 0xff) << 32) | 
                    ((long)(extendedPayloadLen[4] & 0xff) << 24) | 
                    ((long)(extendedPayloadLen[5] & 0xff) << 16) | 
                    ((long)(extendedPayloadLen[6] & 0xff) << 8) | 
                     (long)(extendedPayloadLen[7] & 0xff)
                );
                payloadLenRead = false;
                if (!webSocketInput.hasMask())
                    maskingKeyRead = true;
                else
                    extendedPayloadLenRead = true;
            }
        }
    }

    private void parseExtendedPayloadLenRead(int byteValue){
        if (webSocketInput.hasMask()){
            if (maskingKeyBytesRead==0){
                maskingKey = new byte[4];
                maskingKey[0] = (byte)byteValue;
                maskingKeyBytesRead++;
            }
            else{
                maskingKey[maskingKeyBytesRead] = (byte)byteValue;
                maskingKeyBytesRead++;
                if (maskingKeyBytesRead==4){
                    webSocketInput.setMaskingKey(maskingKey);
                    extendedPayloadLenRead = false;
                    maskingKeyRead = true;
                }
            }
        }
    }
    
    private boolean parseMaskingKeyRead(int byteValue){
        boolean parsingDone = false;
        if (payloadBytesRead==0){
            //Forced conversion to int...
            //Payload length might be a long (64 bits) value, but
            //here it is forcibly converted into an int (32 bits).
            //However, this shouldn't cause problems since payloads
            //of this size aren't used.
            payloadData = new byte[(int)webSocketInput.getPayloadLen()];
            payloadData[0] = (byte)byteValue;
            payloadBytesRead++;
        }
        else{
            payloadData[payloadBytesRead] = (byte)byteValue;
            payloadBytesRead++;
            if (payloadBytesRead==webSocketInput.getPayloadLen()){
                if (webSocketInput.getOpcode()==WebSocketOpcode.TEXT){
                    webSocketInput.setPayloadText(payloadData);
                }
                else if (webSocketInput.getOpcode()==WebSocketOpcode.BINARY){
                    webSocketInput.setPayloadBinary(payloadData);
                }
                else{
                    webSocketInput.setPayloadBinary(payloadData);
                }
                //TODO: Support CONTINUATION Opcode
                parsingDone = true;
            }
        }
        return parsingDone;
    }
    
    public WebSocketDataFrame getWebSocketInput(){
        return webSocketInput;
    }
    
}
