package com.chat3.server.model;

public class HttpRequest {
    
    private String method;
    private String uri;
    private String httpVersion;
    
    private String webSocketKey;
    
    private HttpRequest(String method, String uri, String httpVersion, String webSocketKey){
        this.method = method;
        this.uri = uri;
        this.httpVersion = httpVersion;
        this.webSocketKey = webSocketKey;
    }
    
    public static HttpRequest parse(String request){
        String method = null;
        String uri = null;
        String httpVersion = null;
        String webSocketKey = null;
        
        String temp = request;
        String line;
        
        line = (temp.indexOf("\r\n")!=-1) ? temp.substring(0,temp.indexOf("\r\n")) : temp;
        while (!"".equals(line)){
            temp = temp.substring(line.length()+2); // \r + \n
            
            if (line.startsWith("GET")){
                method = line.substring(0, line.indexOf(" ")).trim();
                
                line = line.substring(line.indexOf(" ")+1, line.length());
                uri = line.substring(0, line.indexOf(" ")).trim();
                
                line = line.substring(line.indexOf(" ")+1, line.length());
                httpVersion = line.trim();
            }
            else if (line.startsWith("Sec-WebSocket-Key")){
                webSocketKey = line.substring(line.indexOf(" "), line.length()).trim();
            }
            
            line = (temp.indexOf("\r\n")!=-1) ? temp.substring(0,temp.indexOf("\r\n")) : temp;
        }
        
        return new HttpRequest(method,uri,httpVersion,webSocketKey);
    }

    public String getMethod() {
        return method;
    }
    public String getUri() {
        return uri;
    }
    public String getHttpVersion() {
        return httpVersion;
    }
    public String getWebSocketKey() {
        return webSocketKey;
    }
}
