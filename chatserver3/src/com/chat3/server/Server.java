package com.chat3.server;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import com.chat3.server.model.WebSocketDataFrame;

public class Server {
    private boolean isRunning = false;
    
    private ServerSocketConnection serverSocket;
    private Thread serverThread;
    
    private ClientListener[] clientConnections = new ClientListener[3];
    
    public void start(){
        if (isRunning)
            throw new IllegalStateException("Server has already been started");
        
        ServerSocketConnection serverConn = connectToPort(80);
        if (serverConn!=null){
            startListeningThread(this,serverConn);
            this.serverSocket = serverConn;
        }
    }
    
    private ServerSocketConnection connectToPort(int listeningPort){
        ServerSocketConnection serverConn = null;
        try {
            serverConn = (ServerSocketConnection) Connector.open("socket://:" + listeningPort);
        } catch (IOException e) { //NOSONAR
            e.printStackTrace(); //NOSONAR
        }
        return serverConn;
    }
    
    public void startListeningThread(final Server server, final ServerSocketConnection serverSocket){
        isRunning = true;
        serverThread = new ListeningThread(server,serverSocket);
        serverThread.start();
    }
    private class ListeningThread extends Thread {
        
        private Server server;
        private ServerSocketConnection serverSocket;
        
        public ListeningThread(Server server,ServerSocketConnection serverSocket){
            this.server = server;
            this.serverSocket = serverSocket;
        }

        public void run() { //NOSONAR
            try {
                System.out.println("Server at "+serverSocket.getLocalAddress()+":"+serverSocket.getLocalPort()); //NOSONAR
            } catch (IOException e) { //NOSONAR
                e.printStackTrace(); //NOSONAR
            }
            
            while (isRunning) {
                try{
                    System.out.println("Waiting clients"); //NOSONAR
                    SocketConnection conn = (SocketConnection) serverSocket.acceptAndOpen();

                    int slot = getEmptyClientSlot();
                    if (slot!=-1){
                        clientConnections[slot] = new ClientListener(server,conn,slot);
                        System.out.println("Client "+conn.getAddress()+":"+conn.getPort()+" at slot "+slot); //NOSONAR
                        Thread t = new Thread(clientConnections[slot]);
                        t.start();
                    }
                    else{
                        conn.close();
                        System.out.println("New connection closed because no slots are available"); //NOSONAR
                    }
                } catch (IOException e) { //NOSONAR
                    System.out.println("An error ocurred with a client: " + e.getMessage()); //NOSONAR
                }
            }
        }
        
    }
    
    public void disconnectFromPort(){
        try {
            serverSocket.close();
        } catch (IOException e) { //NOSONAR
            e.printStackTrace(); //NOSONAR
        }
    }
    
    public void broadcast(String message){
        byte[] dataFrame = new WebSocketDataFrame(message).createResponseDataFrame();
        
        for (int i=0;i<clientConnections.length;i++){
            if (clientConnections[i]!=null && clientConnections[i].hasHandshaked()){
                clientConnections[i].send(dataFrame);
            }
        }
    }
    
    private int getEmptyClientSlot(){
        for (int i=0;i<clientConnections.length;i++){
            if (clientConnections[i]==null)
                return i;
        }
        return -1;
    }
    
    public void freeSlot(int slot){
        clientConnections[slot] = null;
        System.out.println("Client from slot "+slot+" removed"); //NOSONAR
    }
    
    public boolean isRunning(){
        return isRunning;
    }
}
