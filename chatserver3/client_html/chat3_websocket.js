var wsUri = "ws://10.10.26.242/ws/chat";
var websocket;
var conversationArea;

function init(){
	conversationArea = document.getElementById("conversationArea");
	
	websocket = new WebSocket(wsUri);
	websocket.onopen = function(event){ onOpen(event) };
	websocket.onclose = function(event){ onClose(event) };
	websocket.onerror = function(event){ onError(event) };
	websocket.onmessage = function(event){ onMessageReceived(event) };
}

function onOpen(event){
	var jsonData = {"username" : document.getElementById("username").value,
					"event" : "connected"};
	var jsonText = JSON.stringify(jsonData);
	websocket.send(jsonText);
}
function onClose(event){
	writeToScreen("You have been disconnected.","green");
}
function onError(event){
	writeToScreen("Error: "+event.data);
}
function onMessageReceived(event){
	var jsonData = JSON.parse(event.data);
	var message;
	var color;
	if ("message" in jsonData){
		message = jsonData["username"]+" - "+jsonData["message"];
		color = "black";
	}
	else if ("old_username" in jsonData){
		message = "User '"+jsonData["old_username"]+"' has changed his name to '"+jsonData["new_username"]+"'.";
		color = "red";
	}
	else{
		message = "User '"+jsonData["username"]+"' has "+jsonData["event"]+".";
		color = "blue";
	}
	writeToScreen(message,color);
}

function writeToScreen(message,color){
	var newMessage = document.createElement("p");
	newMessage.innerHTML = "<font color=\""+color+"\">"+message+"</font>";
	conversationArea.appendChild(newMessage);
}

function sendMessage(){
	var jsonData = {"username" : document.getElementById("username").value,
					"message" :  document.getElementById("message").value};
	var jsonText = JSON.stringify(jsonData);
	websocket.send(jsonText);
	
	document.getElementById("message").value = "";
}

window.addEventListener("load",init,false);