package Sensors;

import java.util.Scanner;

public class MainOfSensors { //to run all 4 sensors simultaneously

  public static void main(String[] args) throws ArrayIndexOutOfBoundsException {
    String IP = "";
    boolean testModeIsSet = false;
    int controlStartSensorGroup = Integer.valueOf(args[0]);
    int port = Integer.valueOf(args[1]);

    while (true){
      System.out.println("1. localhost");
      System.out.println("2. enter IP");
      System.out.println("T as second char-input - sets Test-mode (e.g.: 1T)");
      Scanner reader = new Scanner(System.in);
      String i = reader.next();
      //reader.close();
      System.out.println(i);
      if (i.contains("T")){
        testModeIsSet = true;
      }
      if(i.contains("1") ){
        IP = "localhost";
        break;
      } else if (i.contains("2")) {
        System.out.println("Please enter IP address: ");
        IP = reader.next();
        break;
      }
    }

    if(controlStartSensorGroup == 0){
      SensorDirectionThread sensorDirection = new SensorDirectionThread(IP, port, testModeIsSet);
      SensorPositionThread sensorPosition = new SensorPositionThread(IP, port, testModeIsSet);
      SensorSpeedThread sensorSpeed = new SensorSpeedThread(IP, port, testModeIsSet);
      sensorDirection.start();
      System.out.println("Direction - Sensor 3 starts");
      sensorPosition.start();
      System.out.println("Position - Sensor 1 starts");
      sensorSpeed.start();
      System.out.println("Speed - Sensor 2 starts");
    } else if (controlStartSensorGroup == 1){
      SensorRoadConditionsThread sensorRoadCondition = new SensorRoadConditionsThread(IP, port, testModeIsSet);
      sensorRoadCondition.start();
      System.out.println("RoadConditions - Sensor 4 starts");
    }
  }
}
