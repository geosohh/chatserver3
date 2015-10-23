package com.chat3.server.model;

public class WebSocketOpcode {
    public static final int CONTINUATION = 0x0;
    public static final int TEXT = 0x1;
    public static final int BINARY = 0x2;
    public static final int CONNECTION_CLOSE = 0x8;
    public static final int PING = 0x9;
    public static final int PONG = 0xA;
    
    private WebSocketOpcode(){
    }
}
