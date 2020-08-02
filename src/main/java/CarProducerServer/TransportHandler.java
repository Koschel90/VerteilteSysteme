package CarProducerServer;

import static java.lang.Thread.sleep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class TransportHandler implements TransportService.Iface{

  private final ArrayList<String> dataFor_ProducerServer_2 = new ArrayList();
  private final ArrayList<String> dataFor_ProducerServer_3 = new ArrayList();
  private ArrayList<String> dataFor_TrafficInfoService_PositionOfCars = new ArrayList();

  private int numberOfDataFor_ProducerServer_2;
  private int numberOfDataFor_ProducerServer_3;
  private int numberOfDataFor_TrafficInfoService;

  static boolean ThreadForSendingToSlavesWasStarting = false;

  public void write(List<String> data) throws org.apache.thrift.TException{
    for(int i=1; i < data.size();i++){//Ausgabe der empfangenen Daten
      System.out.println(data.get(i));
    }
      insertDataInFile(data);
  }

  public void insertDataInFile(List<String> data){
    PrintWriter pWriter = null;
    try { //Daten in File speichern
      pWriter = new PrintWriter(new BufferedWriter(new FileWriter("src/main/java/CarProducerServer/" + data.get(0) + ".txt", true)));
      for(int i = 1; i < data.size(); i++) {
        pWriter.print(data.get(i) + "\n");
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      if (pWriter != null){
        pWriter.flush();
        pWriter.close();
      }
    }
    //Daten für Slaves in Puffer speichern:
    if(data.get(0).equals("data_ProducerServer_1")){
      for(int i = 1; i < data.size(); i++) {
        synchronized (dataFor_ProducerServer_2) {
          dataFor_ProducerServer_2.add(data.get(i));
        }
        synchronized (dataFor_ProducerServer_3){
          dataFor_ProducerServer_3.add(data.get(i));
        }
      }
      //zu Slaves Daten senden starten:
      if(!ThreadForSendingToSlavesWasStarting){
        sendingDataToSlave_ProducerServer_2.start();
        sendingDataToSlave_ProducerServer_3.start();
        sendingDataTo_TrafficInfoService.start();
        ThreadForSendingToSlavesWasStarting = true;
      }
      //PositionsDaten für TrafficInfoService speichern:
      synchronized (dataFor_TrafficInfoService_PositionOfCars) {
        savePositionData(data);
      }
    }
  }

  public void savePositionData(List<String> data){
    for(int i = 1; i < data.size(); i++){
      if(data.get(i).contains("position")){
        dataFor_TrafficInfoService_PositionOfCars.add(data.get(i));
      }
    }
  }

  Thread sendingDataTo_TrafficInfoService = new Thread(new Runnable() {
    @Override
    public void run() {
      while(true){
        String data;
        synchronized (dataFor_TrafficInfoService_PositionOfCars) {
          data = getDataForTrafficInfoService();
        }
        mqtt_pub(data);
        try {
          sleep(4000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });

  Thread sendingDataToSlave_ProducerServer_2 = new Thread(new Runnable() {
    @Override
    public void run() {
      while(true){
        ArrayList<String> data = getDataForProducer("ProducerServer_2");
        if(data.size() != 0) {
          sendData(data, "ProducerServer_2", 9899);
        }
        try {
          sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });

  Thread sendingDataToSlave_ProducerServer_3 = new Thread(new Runnable() {
    @Override
    public void run() {
      while(true){
        ArrayList<String> data = getDataForProducer("ProducerServer_3");
        if(data.size() != 0) {
          sendData(data, "ProducerServer_3", 9900);
        }
        try {
          sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });

  public ArrayList<String> getDataForProducer(String slaveName){
    ArrayList<String> tmp = new ArrayList();
    ArrayList<String> data = new ArrayList<>();
    if(slaveName == "ProducerServer_2"){
      synchronized (dataFor_ProducerServer_2) {
        tmp = dataFor_ProducerServer_2;
      }
    } else if (slaveName == "ProducerServer_3"){
      synchronized (dataFor_ProducerServer_3) {
        tmp = dataFor_ProducerServer_3;
      }
    } else {
      System.out.println("Error by get data for sending");
    }
    //Daten in ein String packen:
      int size = tmp.size();
      int numberOfDataFor_ProducerServer;
      data.add("data_" + slaveName);
      if (size > 10) {
        //10 send:
        numberOfDataFor_ProducerServer = 10;
        for (int i = 0; i < 10; i++) {
          data.add(tmp.get(i));
        }
      } else {
        numberOfDataFor_ProducerServer = size;
        for (int i = 0; i < size; i++) {
          data.add(tmp.get(i));
        }
      }
      //Anzahl der Daten, welche verschickt werden:
    if(slaveName == "ProducerServer_2"){
      numberOfDataFor_ProducerServer_2 = numberOfDataFor_ProducerServer;
    } else if (slaveName == "ProducerServer_3") {
      numberOfDataFor_ProducerServer_3 = numberOfDataFor_ProducerServer;
    }
    return data;
  }

  public void sendData(ArrayList<String> data, String slaveName, int port){
    TTransport transport;

    try {
      transport = new TSocket("localhost", port);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      TransportService.Client client = new TransportService.Client(protocol);
      client.write(data);
      transport.close();
      //remove data which was send:
      if(slaveName == "ProducerServer_2"){
        synchronized (dataFor_ProducerServer_2){
          for (int i = 0; i < numberOfDataFor_ProducerServer_2; i++) {
            dataFor_ProducerServer_2.remove(0);
          }
        }
      } else if (slaveName == "ProducerServer_3"){
        synchronized (dataFor_ProducerServer_3){
          for (int i = 0; i < numberOfDataFor_ProducerServer_3; i++) {
            dataFor_ProducerServer_3.remove(0);
          }
        }
      }
    } catch (TTransportException e) {
      e.printStackTrace();
    } catch (TException e) {
      e.printStackTrace();
    }
  }

  public void mqtt_pub(String data){ //über mqtt versenden:
    try{
      System.out.println("Sending data:        " + data);
      MqttClient client = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
      client.connect();
      MqttMessage message = new MqttMessage(data.getBytes());
      message.setQos(2); //Quality of Service (2): exactly once
      client.publish("CarPosition", message);
      client.disconnect();
      synchronized (dataFor_TrafficInfoService_PositionOfCars) {
        for (int i = 0; i < numberOfDataFor_TrafficInfoService; i++){
          dataFor_TrafficInfoService_PositionOfCars.remove(0);
        }
      }
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  public String getDataForTrafficInfoService(){
    String data = "";
    int size = dataFor_TrafficInfoService_PositionOfCars.size();
    if(size > 5){
      for(int i = 0; i < 5; i++){
        data += dataFor_TrafficInfoService_PositionOfCars.get(i);
        data += ";";
        numberOfDataFor_TrafficInfoService = 5;
      }
    }else {
      for(int i = 0; i < size; i++){
        data += dataFor_TrafficInfoService_PositionOfCars.get(i);
        data += ";";
        numberOfDataFor_TrafficInfoService = size;
      }
    }
    return data;
  }

}
