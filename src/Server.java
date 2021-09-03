// Java server program for ANU's comp3310 sockets Lab 
// Peter Strazdins, RSCS ANU, 03/18

import java.io.*;
import java.net.*;

public class Server {
    static int port = 3310;
    static final int BUFLEN = 64;
    public static void main (String[] args) throws IOException {
	if (args.length >= 1)
	    port = Integer.parseInt(args[0]);
        //ServerSocket sSock = new ServerSocket(port);
        DatagramSocket datagramSocket = new DatagramSocket(port);
        System.out.println("server: created socket with port number " +
			   datagramSocket.getPort());
    while (true) {
        byte[] inData = new byte[BUFLEN];
        DatagramPacket inpacket = new DatagramPacket(inData, inData.length);
        datagramSocket.receive(inpacket);
        String inmsg = new String(inpacket.getData());
        int Inport = inpacket.getPort();
        InetAddress address = inpacket.getAddress();   //get the address of received packet
        System.out.println("erver : received message from "+ address+ " on port "+Inport+": "+inmsg);

        DatagramPacket outPacket = new DatagramPacket(inData, inData.length, address, Inport);
        datagramSocket.send(outPacket);
    }
    }//main()
}//Server
