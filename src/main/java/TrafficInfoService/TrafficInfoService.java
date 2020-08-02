package TrafficInfoService;

import static java.lang.Thread.sleep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class TrafficInfoService {

  private final ArrayList<String> rawData = new ArrayList();
  private ArrayList<Car> allCarsWithCurrentData = new ArrayList();
  private ArrayList<String> dataToSendToCars = new ArrayList();

  public class Car {
    private Integer ID;
    private Integer position;
    private long timestamp;

    public Car(Integer ID, Integer position, long timestamp){
      this.ID = ID;
      this.position = position;
      this.timestamp = timestamp;
    }

    public Integer getID() {
      return ID;
    }

    public void setPosition(Integer position) {
      this.position = position;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    public Integer getPosition() {
      return position;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  public void run(){
      mqtt_subFromProducerServer_1(); //Daten empfangen
      processData.start();//Daten bearbeiten
      sendCurrentTrafficToCars.start(); //Daten senden
  }

  public void mqtt_subFromProducerServer_1(){ //Daten empfangen von ProducerServer_1
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
          //Daten extrahieren:
          data = extractData(receivedData);
          //in File schreiben:
          insertDataInFile(data);
          synchronized (rawData){
            for(int i = 0; i < data.size(); i++){
              rawData.add(data.get(i));
            }
          }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
      });

      client.connect();
      client.subscribe("CarPosition");
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  public void currentCarData(ArrayList<String> data){
    //jeden neuen Datensatz durchgehen:
    for(int i = 0; i < data.size(); i++){
      //Daten extrahieren zur Bearbeitung:
      String[] splittedData = data.get(i).split(" ");
      int carID = Integer.parseInt(splittedData[1]);
      int position = Integer.parseInt(splittedData[3]);
      long timestamp = Long.parseLong(splittedData[5]);;
      boolean carIDIsKnown = false;
      int indexOfFoundCar = -1;
      for(int j = 0; j < allCarsWithCurrentData.size(); j++) {//Überprüfung, ob CarID schon bekannt?
        if(allCarsWithCurrentData.get(j).getID() == carID){
          indexOfFoundCar = j;
          carIDIsKnown = true;//bekannt
          break;
        }
      }
      if(carIDIsKnown){
        if(timestamp > allCarsWithCurrentData.get(indexOfFoundCar).getTimestamp()){
          allCarsWithCurrentData.get(indexOfFoundCar).setPosition(position);
          allCarsWithCurrentData.get(indexOfFoundCar).setTimestamp(timestamp);
          calculateCurrentTraffic(indexOfFoundCar);
        }
      } else { //neues Car Objekt anlegen
        Car c = new Car(carID, position, timestamp);
        allCarsWithCurrentData.add(c);
        calculateCurrentTraffic(allCarsWithCurrentData.size()-1);
      }
    }
  }

  public void calculateCurrentTraffic (int indexOfCarWithNewData){
    ArrayList<Car> carsNearby = new ArrayList();
    ArrayList<Car> accidentWithThisCars = new ArrayList();
    for(int i = 0; i < allCarsWithCurrentData.size(); i++){
      if(i != indexOfCarWithNewData){//Überspringen des gleichen Autos
        Car carWithNewData = allCarsWithCurrentData.get(indexOfCarWithNewData);
        Car carToCompare = allCarsWithCurrentData.get(i);
        int carWithNewPosition = carWithNewData.getPosition();
        int carPositionToCompare = carToCompare.getPosition();
        //Überprüfen, ob auto in der Nähe(ein Feld nebendran):
        boolean isNearby = isCarNearbyOfTheOtherCar(carWithNewPosition, carPositionToCompare);
        if(isNearby == true){
          synchronized (dataToSendToCars) {
            String s = carWithNewData.getID() + " ";
            s += "Car " + carToCompare.getID() + " is nearby at position " + carToCompare.getPosition() + "(timestamp: ";
            s += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(carWithNewData.getTimestamp())) + ")";
            dataToSendToCars.add(s);
            s = carToCompare.getID() + " ";
            s += "Car " + carWithNewData.getID() + " is nearby at position " + carWithNewData.getPosition() + "(timestamp: ";
            s += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(carToCompare.getTimestamp())) + ")";
            dataToSendToCars.add(s);
          }
          System.out.println("Car " + carWithNewData.getID() + " and Car " + carToCompare.getID() + " are next to each other!");
        }
        //Überprüfen, ob car auf gleicher Position ist = Unfall:
        if( carPositionToCompare == carWithNewPosition){
          synchronized (dataToSendToCars) {
            String s = carWithNewData.getID() + " ";
            s += "you have an accident with Car " + carToCompare.getID();
            dataToSendToCars.add(s);
            s = carToCompare.getID() + " ";
            s += "you have an accident with Car " + carWithNewData.getID();
            dataToSendToCars.add(s);
          }
          System.out.println("Car " + carWithNewData.getID() + " and Car " + carToCompare.getID() + " have an accident!");
        }
      }
    }

  }

  public boolean isCarNearbyOfTheOtherCar(int positionNew, int positionToCompare){
    boolean isNearby = false;
    if((positionNew + 5) == positionToCompare || (positionNew - 20) == positionToCompare){ //Süden
      isNearby = true;
    } else if ((positionNew + -5) == positionToCompare || (positionNew + 20) == positionToCompare){ //Norden
      isNearby = true;
    } else if ((positionNew + 1) == positionToCompare || (positionNew - 5) == positionToCompare){ //Osten
      isNearby = true;
    } else if ((positionNew - 1) == positionToCompare || (positionNew + 5) == positionToCompare){ //Westen
      isNearby = true;
    }
    return isNearby;
  }

  public ArrayList<String> extractData(String data) throws ParseException {
    ArrayList<String> extractedData = new ArrayList();
    while(data.contains(";")){ //einzelne Datensätze extrahieren:
      String tmp1 = data.substring(0, data.indexOf(';'));
      String tmp2 = tmp1.substring(0, 6) + " " + tmp1.substring(6,11);
      tmp2 += tmp1.substring(tmp1.indexOf('p'), tmp1.indexOf('-'));
      String time = tmp1.substring(tmp1.indexOf('-') + 2, tmp1.length());
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
      Date parsedDate = dateFormat.parse(time);
      tmp2 += "timestamp: " + parsedDate.getTime(); //Zeit in Millisekunden
      extractedData.add(tmp2);//vordersten Datensatz abspeichern
      data = data.substring(data.indexOf(';') + 1, data.length()); //vordersten Datensatz löschen
    }
    return extractedData;
  }

  public void insertDataInFile(ArrayList<String> data){
    PrintWriter pWriter = null;
    try { //Daten in File speichern
      pWriter = new PrintWriter(new BufferedWriter(new FileWriter("src/main/java/TrafficInfoService/" + "data_TrafficInfoService.txt", true)));
      synchronized ((data)) {
        for (int i = 0; i < data.size(); i++) {
          pWriter.print(data.get(i) + "\n");
        }
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      if (pWriter != null){
        pWriter.flush();
        pWriter.close();
      }
    }
  }

  public void mqtt_pub(String data, String carID){ //über mqtt versenden:
    try{
      MqttClient client = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
      client.connect();
      MqttMessage message = new MqttMessage(data.getBytes());
      message.setQos(2); //Quality of Service (2): exactly once
      client.publish("currentTraffic" + carID, message);
      client.disconnect();
      System.out.println("###### Senden daten: " + data + "(mit id:" + carID + ")");
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  Thread sendCurrentTrafficToCars = new Thread(new Runnable() {
    @Override
    public void run() {
      while(true) {
        synchronized (dataToSendToCars) {
          if (dataToSendToCars.size() != 0) {
            for (int i = 0; i < dataToSendToCars.size(); i++) {
              String carID = dataToSendToCars.get(0).substring(0, 4);
              String data = dataToSendToCars.get(0).substring(4, dataToSendToCars.get(0).length());
              mqtt_pub(data, carID);
              dataToSendToCars.remove(0);//gesendete Elemente löschen
            }
          }
        }//end of synchronized
      }
    }
  });


  Thread processData = new Thread(new Runnable() {
    @Override
    public void run() {
      while (true) {
        //aktuelle Postion pro Auto speichern
        ArrayList<String> data = new ArrayList();
        synchronized (rawData) {
          if (rawData.size() != 0) {
            data = rawData;
            for (int i = 0; i < data.size(); i++) {
              System.out.println(rawData.get(0));
              rawData.remove(0);
            }
          }
        }
        if (data.size() != 0) {
          currentCarData(data);
        }
        try {
          sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });

  public static void main(String[] args) {
    TrafficInfoService t = new TrafficInfoService();
    t.run();
    System.out.println("Starting Traffic Information Service ...");
  }
}
