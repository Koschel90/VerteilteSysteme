package Sensors;

import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class SensorRoadConditionsThread extends Thread {

  private Sensors sensor;
  private boolean testMode;

  SensorRoadConditionsThread(String IP, int port, boolean mode) {
    this.sensor = new Sensors(IP, port, "roadConditions");
    this.testMode = mode;
  }

  public void run() {
    while (true) {
      try {
        sleep(2000); //2 sec
        String data = getData();
        sensor.sendUDPPacket(data);
        System.out.println(data);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private String getData(){
    //generate data:
    String data = "0";
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    if(!testMode) {
      int randomNum = ThreadLocalRandom.current().nextInt(4) + 1;
      if (randomNum == 1) {
        data = "wet";
      } else if (randomNum == 2) {
        data = "dry";
      } else if (randomNum == 3) {
        data = "smooth";
      } else if (randomNum == 4) {
        data = "slippery";
      }
    } else {
      data = "fgehr";
    }
    String typ = "4";
    String name = " Sensor of roadConditions: ";
    String unit = " - ";
    return typ + name + data + unit + timestamp;
  }
}
