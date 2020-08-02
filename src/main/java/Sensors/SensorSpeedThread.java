package Sensors;

import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class SensorSpeedThread extends Thread {

  private Sensors sensor;
  private boolean testMode;

  SensorSpeedThread(String IP, int port, boolean mode) {
    this.sensor = new Sensors(IP, port, "Speed");
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
    String data = "-1";
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    if(!testMode) {
      int randomNum = ThreadLocalRandom.current().nextInt(2);
      if (randomNum == 0) {
        data = "0";
      } else if (randomNum == 1) {
        data = "1";
      }
    } else {
      data = "-1000000000";
    }
    String typ = "2";
    String name = " Sensor of speed: ";
    String unit = " mph ";
    SensorPositionThread.setSpeed(data); //transfer of data
    return typ + name + data + unit + timestamp;
  }
}
