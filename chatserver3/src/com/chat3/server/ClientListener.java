package com.chat3.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.SocketConnection;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import com.chat3.server.model.HttpRequest;
import com.chat3.server.model.WebSocketDataFrame;
import com.chat3.server.model.WebSocketDataFrameParser;
import com.chat3.server.model.WebSocketOpcode;

import net.intertwingly.SHA1;

public class ClientListener implements Runnable {
    private SocketConnection socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String currentUsername = "";
    private boolean hasHandshaked = false;
    private boolean isConnected = true;
    private Server server;
    private int slot;
    
    private static final String JSON_EVENT = "event";
    private static final String JSON_USERNAME = "username";
    
    public ClientListener(Server server, SocketConnection socket, int slot) throws IOException{
        this.socket = socket;
        inputStream = socket.openInputStream();
        outputStream = socket.openOutputStream();
        
        this.server = server;
        this.slot = slot;
    }

    public void run() { //NOSONAR
        String text;
        WebSocketDataFrame data;
        while (server.isRunning() && isConnected) {
            
            // Awaits client input then reads it.
            if (!hasHandshaked){ // http
                text = readHttpInput();
                handleHttpRequest(text);
            }
            
            else{ // websocket
                data = readWebSocketInput();
                handleWebSocketRequest(data);
            }
        }
    }
    
    private String readHttpInput(){
        StringBuffer buffer = new StringBuffer(); //NOSONAR
        int[] inputEnd = {-1,-1,-1,-1};
        
        int byteValue;
        try {
            while ((byteValue = inputStream.read()) != -1){
                // SOCKET connections doesn't send EOF, so it's necessary 
                // to check for an empty line (i.e. end of HTTP message)
                buffer.append((char)byteValue);
                inputEnd[0] = inputEnd[1];
                inputEnd[1] = inputEnd[2];
                inputEnd[2] = inputEnd[3];
                inputEnd[3] = byteValue;
                if (inputEnd[0]==13 && inputEnd[1]==10 && inputEnd[2]==13 && inputEnd[3]==10)
                    break;
            }
        } catch (IOException e) {
            resolveInputException(e);
        }
        return buffer.toString();
    }

    private WebSocketDataFrame readWebSocketInput(){
        WebSocketDataFrameParser webSocketParser = new WebSocketDataFrameParser();
        
        int byteValue = -1;
        do{
            try{
                byteValue = inputStream.read();
            } catch(IOException e){
                byteValue = -1;
                resolveInputException(e);
            }
        }while(byteValue!=-1 && webSocketParser.parse(byteValue));
        
        return webSocketParser.getWebSocketInput();
    }
    
    private void resolveInputException(Exception e){
        if (e.getMessage().endsWith("10053") || 
            e.getMessage().endsWith("10054") ||
            e.getMessage().endsWith("Stream closed")){
            closeConnection();
        }
        else{
            e.printStackTrace(); //NOSONAR
        }
    }
    
    public void handleHttpRequest(String requestText){
        HttpRequest request = HttpRequest.parse(requestText);
        if ("GET".equals(request.getMethod())){
            if ("/".equals(request.getUri())){
                String msg = "HTTP/1.1 200 OK\n"
                            +"Accept-Ranges: bytes\n"
                            +"Content-Length: 97\n"
                            +"Connection: close\n"
                            +"Content-Type: text/html\n"
                            +"\n"
                            +"<html>"
                            +"<head><link rel=\"shortcut icon\" href=\"#\"/></head>"
                            +"<body><h1>It works!</h1></body>"
                            +"</html>";
                send(msg);
            }
            else if ("/ws/chat".equals(request.getUri()) && "HTTP/1.1".equals(request.getHttpVersion())){
                if (request.getWebSocketKey()!=null){
                    handshakeResponse(request.getWebSocketKey());
                }
                else{
                    //"If any header is not understood or has an incorrect value, the server should send a 
                    //'400 Bad Request' and immediately close the socket."
                    //https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
                    String msg = "HTTP/1.1 400 Bad Request\n\n";
                    send(msg);
                }
            }
            else{
                System.out.println("Received GET message: \"" + requestText + "\""); //NOSONAR
            }
        }
        else{
            System.out.println("Received a message: \"" + requestText + "\""); //NOSONAR
        }
    }
    public void handleWebSocketRequest(WebSocketDataFrame data){
        if (!data.hasMask())
            webSocketRequestCloseConnection();
        if (data.getOpcode()==WebSocketOpcode.CONNECTION_CLOSE)
            webSocketRespondCloseConnection();
        else if (data.getOpcode()==WebSocketOpcode.PING)
            webSocketPong(data.getPayloadBinary());
        else if (data.getOpcode()==WebSocketOpcode.TEXT)
            webSocketMessageReceived(data.getPayloadText());
    }
    
    private void handshakeResponse(String key){
        String fullKey = key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        String acceptKey = SHA1.encode(fullKey);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"+
                          "Upgrade: websocket\r\n"+
                          "Connection: Upgrade\r\n"+
                          "Sec-WebSocket-Accept: "+acceptKey+"\r\n"+
                          "\r\n";
        send(response);
        hasHandshaked = true;
    }
    
    private void send(String message){
        send(message.getBytes());
    }
    public void send(byte[] messageBytes){
        try {
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (IOException e) {
            resolveInputException(e);
        }
    }
    
    private void webSocketMessageReceived(String message){
        try {
            JSONObject jsonData = new JSONObject(message);
            if (jsonData.has(JSON_EVENT) && "connected".equals(jsonData.getString(JSON_EVENT))){
                currentUsername = jsonData.getString(JSON_USERNAME);
            }
            else{
                String newUsername = jsonData.getString(JSON_USERNAME);
                if (!newUsername.equals(currentUsername)){
                    String messageNameChanged = new JSONObject().put("old_username", currentUsername)
                                                                .put("new_username", newUsername)
                                                                .toString();
                    server.broadcast(messageNameChanged);
                    currentUsername = newUsername;
                }
            }
            server.broadcast(message);
        } catch (JSONException e) { //NOSONAR
            e.printStackTrace(); //NOSONAR
        } 
    }
    
    private void webSocketRequestCloseConnection(){
        //TODO: "section 5.1 of the spec says that your server must disconnect 
        //TODO: from a client if that client sends an unmasked message"
        //TODO: http://tools.ietf.org/html/rfc6455#section-1.4
    }
    
    private void webSocketRespondCloseConnection(){
        //http://tools.ietf.org/html/rfc6455#section-5.5.1
        WebSocketDataFrame response = new WebSocketDataFrame("");
        response.setOpcode(WebSocketOpcode.CONNECTION_CLOSE);
        byte[] dataFrame = response.createResponseDataFrame();
        send(dataFrame);
        
        try {
            String messageUserDisconnected = new JSONObject().put(JSON_USERNAME, currentUsername)
                                                             .put(JSON_EVENT, "disconnected")
                                                             .toString();
            server.broadcast(messageUserDisconnected);
        } catch (JSONException e) { //NOSONAR
            e.printStackTrace(); //NOSONAR
        }
        closeConnection();
    }
    
    private void webSocketPong(byte[] payloadData){ //NOSONAR
        //TODO: http://tools.ietf.org/html/rfc6455#section-5.5.3
    }
    
    private void closeConnection(){
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
            isConnected = false;
        } catch (IOException e1) { //NOSONAR
            e1.printStackTrace(); //NOSONAR
        }
        server.freeSlot(slot);
    }
    
    /**
     * Used by Server.broadcast() to only send messages to handshaked clients.
     * */
    public boolean hasHandshaked(){
        return hasHandshaked;
 }
}
