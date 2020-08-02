package Sensors;

import java.sql.Timestamp;

public class SensorPositionThread extends Thread {

  private String IP;
  private int port;

  private static String direction;
  private static int speed;
  private int position = 1;
  private boolean testMode;

  private Sensors sensor;

  SensorPositionThread(String IP, int port, boolean mode) {
    this.sensor = new Sensors(IP, port, "position");
    this.testMode = mode;
  }

  static public void setDirection(String currentDirection){
    direction = currentDirection;
  }

  static public void setSpeed(String currentSpeed){
    try {
      speed = Integer.parseInt(currentSpeed.trim());
    } catch(NumberFormatException nfe) {
        System.out.println("NumberFormatException: " + nfe.getMessage());
      }
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

  private String getData() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    if(!testMode) {
      if (speed == 1) { //car drives
        switch (direction) {
          case "North":
            if (position < 6) {
              position = position + 20; //card ends
            } else {
              position = position - 5;
            }
            break;
          case "East":
            if ((position % 5) == 0) {
              position = position - 4; //card ends
            } else {
              position = position + 1;
            }
            break;
          case "South":
            if (position > 20) {
              position = position - 20; //card ends
            } else {
              position = position + 5;
            }
            break;
          case "West":
            int rightNumeralOfPosition = position % 10;
            if (rightNumeralOfPosition == 1 || rightNumeralOfPosition == 6) {
              position = position + 4;  //card ends
            } else {
              position = position - 1;
            }
            break;
        }
      }
    } else {
      position = -1000;
    }
    String typ = "1";
    String name = " Sensor of position: ";
    String unit = " - ";
    return typ + name + position + unit + timestamp;
  }
}