package WebService;

import CarProducerServer.TransportService;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class CarCentralServer extends Thread {

  static String IP;
  private int port;
  private int carID;
  private int BUFFER_SIZE = 1024;

  static ArrayList<String> sensorPosition = new ArrayList();
  static ArrayList<String> sensorSpeed = new ArrayList();
  static ArrayList<String> sensorDirection = new ArrayList();
  static ArrayList<String> sensorRoadConditions = new ArrayList();

  static ArrayList<String> dataForProducer = new ArrayList();

  static ArrayList<String> trafficInfo = new ArrayList();

  static int numberOfDataForProducer;

  public CarCentralServer(int port, String IP) {
    this.IP = IP;
    this.port = port;
    this.carID = port;
    numberOfDataForProducer = 0;
  }

  public void run() {
    try {
      sendToCarProducerServerThread.start();
      mqtt_subFromTrafficInfoService(); // geht das hier ?
      receivePacket();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void receivePacket() throws Exception {
    DatagramSocket socket = new DatagramSocket(port);
    System.out.println("CarCentralServer starts");

    byte[] receiveData = new byte[BUFFER_SIZE];

    while (true) {
      DatagramPacket packetFromSensor = new DatagramPacket(receiveData, receiveData.length);
      socket.receive(packetFromSensor);

      InetAddress address = packetFromSensor.getAddress();
      int port = packetFromSensor.getPort();
      int len = packetFromSensor.getLength();
      byte[] data = packetFromSensor.getData();

      System.out.println(
          "Request from " + address + " with port " + port + " with length " + len + " - "
              + new String(data, 0, len));
      saveData(new String(data, 0, len));
    }//while ends
  }

  public void saveData(String data) {
    String splittedData[] = data.split(" ");
    if (splittedData[3].contains("position")) {
      int value = Integer.parseInt(splittedData[4]);
      if (value > 0 && value < 26) {
        sensorPosition.add(data);
        synchronized (dataForProducer) {
          dataForProducer.add(data);
        }
      } else {
        System.out.println("packet from sensor position is faulty! invalid value: " + splittedData[4]);
      }
    } else if (splittedData[3].contains("speed")) {
      int value = Integer.parseInt(splittedData[4]);
      if (value == 1 || value == 0) {
        sensorSpeed.add(data);
        synchronized (dataForProducer) {
          dataForProducer.add(data);
        }
      } else {
        System.out.println("packet from sensor speed is faulty! invalid value: " + splittedData[4]);
      }
    } else if (splittedData[3].contains("direction")) {
      if (splittedData[4].equals("North") || splittedData[4].equals("East") || splittedData[4]
          .equals("South") || splittedData[4].equals("West")) {
        sensorDirection.add(data);
        synchronized (dataForProducer) {
          dataForProducer.add(data);
        }
      } else {
        System.out
            .println("packet from sensor direction is faulty! invalid value: " + splittedData[4]);
      }
    } else if (splittedData[3].contains("roadConditions")) {
      if (splittedData[4].equals("wet") || splittedData[4].equals("dry") || splittedData[4]
          .equals("smooth") || splittedData[4].equals("slippery")) {
        sensorRoadConditions.add(data);
        synchronized (dataForProducer) {
          dataForProducer.add(data);
        }
      } else {
        System.out.println(
            "packet from sensor roadConditions is faulty! invalid value: " + splittedData[4]);
      }
    } else {
      System.out.println("Data of received packet is faulty - not saved!");
    }
  }

  public void outputNumberOfReceivedPacket() {
    System.out.println(" ");
    System.out.println("Number of received Packets: " + "Position: " + sensorPosition.size() + " Speed: " + sensorSpeed.size() + " Direction " + sensorDirection.size() + " RoadConditions " + sensorRoadConditions.size());
    System.out.println(" ");
  }

  Thread sendToCarProducerServerThread = new Thread(new Runnable() {
    @Override
    public void run() {
      while(true) {
        int size;
        synchronized (dataForProducer) {
          size = dataForProducer.size();

          if(size != 0) {
            ArrayList<String> data = getDataForProducer();
            sendToCarProducerServer(data);
          }
        }
        try {
          sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });

  static void sendToCarProducerServer(ArrayList<String> data){
    TTransport transport;

    try {
      transport = new TSocket("localhost", 9898);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      TransportService.Client client = new TransportService.Client(protocol);
      client.write(data);
      transport.close();
      //remove data which was send:
      synchronized (dataForProducer) {
        for (int i = 0; i < numberOfDataForProducer; i++) {
          dataForProducer.remove(0);
        }
      }
    } catch (TTransportException e) {
      e.printStackTrace();
    } catch (TException e) {
      e.printStackTrace();
    }
  }

  public ArrayList<String> getDataForProducer(){
    ArrayList<String> data = new ArrayList();
    data.add("data_ProducerServer_1");
   // synchronized (dataForProducer) {
      int size = dataForProducer.size();
      if (size > 10) {
        //10 send:
        for (int i = 0; i < 10; i++) {
          data.add("CarID:" + carID + " data: " + dataForProducer.get(i));
          numberOfDataForProducer = 10;
        }
      } else {
        for (int i = 0; i < size; i++) {
          data.add("CarID:" + carID + " data: " + dataForProducer.get(i));
          numberOfDataForProducer = size;
        }
      }
   // }
    return data;
  }

  public void mqtt_subFromTrafficInfoService(){ //Daten empfangen von ProducerServer_1
    try {
      MqttClient client = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId(), new MemoryPersistence());

      client.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws ParseException {
          ArrayList<String> data = new ArrayList();
          String receivedData = new String(mqttMessage.getPayload());
          System.out.println("Received data: " + receivedData);

          /*if(trafficInfo.size()==0){trafficInfo.add(receivedData);
          }else{
            trafficInfo.remove(0);
            trafficInfo.add(receivedData);
          }*/

          trafficInfo.add(receivedData);
          System.out.println("########## new Traffic Infos : " + receivedData + " ##########");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
      });

      client.connect();
      client.subscribe("currentTraffic" + carID);
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws ArrayIndexOutOfBoundsException {
    String IP = "";
    int port = Integer.valueOf(args[0]);

    while (true){
      System.out.println("Set IP for ProducerServer_1 connection: ");
      System.out.println("1. localhost");
      System.out.println("2. enter IP");
      Scanner reader = new Scanner(System.in);
      String i = reader.next();
      if(i.equals("1") ){
        IP = "localhost";
        break;
      } else if (i.equals("2")) {
        System.out.println("Please enter IP address: ");
        IP = reader.next();
        break;
      }
    }

    CarCentralServer carServer = new CarCentralServer(port, IP);
    carServer.start();
    try {
      ServerSocket tcpConnection = new ServerSocket(port); //Für MainOfSensors
      while (true) {
        Socket socket = tcpConnection.accept();
        CarWebServer webServer = new CarWebServer(socket);
        webServer.start();
      }//while ends
    } catch (Exception e){
      e.printStackTrace();
    }
  }

}

