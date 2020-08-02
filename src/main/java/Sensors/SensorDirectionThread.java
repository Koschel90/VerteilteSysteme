package Sensors;

import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class SensorDirectionThread extends Thread{

  private Sensors sensor;
  private boolean testMode;

  SensorDirectionThread(String IP, int port, boolean mode) {
    this.sensor = new Sensors(IP, port, "direction");
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
    String data = "-1";
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    if(!testMode) {
      int randomNum = ThreadLocalRandom.current().nextInt(4) + 1;
      if (randomNum == 1) {
        data = "North";
      } else if (randomNum == 2) {
        data = "East";
      } else if (randomNum == 3) {
        data = "South";
      } else if (randomNum == 4) {
        data = "West";
      }
    } else {
      data = "USA176";
    }
    String typ = "3";
    String name = " Sensor of direction: ";
    String unit = " - ";
    SensorPositionThread.setDirection(data); //transfer of data
    return typ + name + data + unit + timestamp;
  }
}
