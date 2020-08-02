package Sensors;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sensors {

  private String IP;
  private int port;
  private int BUFFER_SIZE = 1024;
  private DatagramSocket clientSocket;
  private int numberOfSentPacket;
  private String sensorName;

  Sensors(String IP, int port, String sensorName) {
    this.IP = IP;
    this.port = port;
    numberOfSentPacket = 0;
    this.sensorName = sensorName;
    try {
      this.clientSocket = new DatagramSocket(); //create Socket
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendUDPPacket(String data) throws Exception{

    InetAddress IPAddress = InetAddress.getByName(IP);

    byte[] sendData;
    byte[] receiveData = new byte[BUFFER_SIZE];

    //DataString:
    sendData = data.getBytes();

    //1. Sending packet
    // format:
    // data + length of data + IP-address + port
    // (in data: type, name, value, unit)
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
    clientSocket.send(sendPacket);
    numberOfSentPacket++;
    System.out.println("NumberOfAllSentPacketsFromSensor - " + sensorName + ": " + numberOfSentPacket);
/*
    //2. Receive packet - (blocking)
    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    clientSocket.receive(receivePacket);
    //server feedback:
    String modifiedSentence = new String(receivePacket.getData());
    System.out.println("Sensor of position received: " + modifiedSentence.trim());
*/
    //clientSocket.close();
  }
}
