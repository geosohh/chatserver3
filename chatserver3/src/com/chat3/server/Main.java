package com.chat3.server;

// SHA1 Class
// http://www.intertwingly.net/blog/2004/07/18/Base64-of-SHA1-for-J2ME

// JSON for JavaME
// https://bitbucket.org/liedman/json-me/overview

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class Main extends MIDlet {

    private Server server;

    public Main() {
        server = new Server();
    }

    // Override
    protected void destroyApp(boolean arg0) throws MIDletStateChangeException { //NOSONAR
        server.disconnectFromPort();
    }

    // Override
    protected void pauseApp() { //NOSONAR
        server.disconnectFromPort();
    }

    // Override
    protected void startApp() throws MIDletStateChangeException { //NOSONAR
        server.start();
    }

}
