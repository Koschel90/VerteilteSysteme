package WebService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;

public class CarWebServer extends Thread {

  private Socket socket;
  private ArrayList<String> browserClientHistory = new ArrayList();

  CarWebServer(Socket socket){
    this.socket = socket;
  }

  public void run(){
    try{
      listenForTCPPackage();
      //mqtt empfangen von TrafficInfoService und bearbeiten!!!
      //.....

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void listenForTCPPackage(){
    String clientSentence;
    String splittedRequest[];
    String response = "";

    try{
      BufferedReader clientRequest = new BufferedReader(( new InputStreamReader(socket.getInputStream())));
      clientSentence = clientRequest.readLine();
      splittedRequest = clientSentence.split(" ");
      while(!clientSentence.contains("User-Agent")) {
        clientSentence = clientRequest.readLine();
      }

      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      browserClientHistory.add(clientSentence + " " + timestamp);
      //Test:
      for(int i = 0; i < browserClientHistory.size(); i++) {
        System.out.println(browserClientHistory.get(i));
      }

      if(splittedRequest[0].equals("GET")){ //Request of client?
        String nameRequest = splittedRequest[1];
        response = generatePackage(nameRequest);
      } else {
        response = generateResponse("404");
      }
      PrintWriter writer = new PrintWriter(socket.getOutputStream());
      writer.write(response.toCharArray());
      writer.flush();//send

      clientRequest.close(); //closing HTTP reader
      writer.close(); //closing writer
      socket.close(); //closing connection

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public String generatePackage(String nameRequest){
    String responsePackage = "";
    String data;
    if(nameRequest.contains("/Position")){
      data = generateStringOfData(CarCentralServer.sensorPosition, nameRequest);
    } else if(nameRequest.contains("/Speed")){
      data = generateStringOfData(CarCentralServer.sensorSpeed, nameRequest);
    } else if(nameRequest.contains("/Direction")){
      data = generateStringOfData(CarCentralServer.sensorDirection, nameRequest);
    } else if(nameRequest.contains("/RoadConditions")){
      data = generateStringOfData(CarCentralServer.sensorRoadConditions, nameRequest);
    } else if(nameRequest.contains("/Traffic")){
      data = generateStringOfData(CarCentralServer.trafficInfo, nameRequest);
    } else if(nameRequest.contains("/All")){
      data = "<table border=\"1\"><tr>";
      data += "<td>";
      data += generateStringOfData(CarCentralServer.sensorPosition, nameRequest);
      data += "</td><td>";
      data += generateStringOfData(CarCentralServer.sensorSpeed, nameRequest);
      data += "</td><td>";
      data += generateStringOfData(CarCentralServer.sensorDirection, nameRequest);
      data += "</td><td>";
      data += generateStringOfData(CarCentralServer.sensorRoadConditions, nameRequest);
      data += "</td>";
      data += "</tr></table>";
    } else{
      data = "404";
    }
    responsePackage = generateResponse(data);
    return responsePackage;
  }

  public String generateResponse(String data) {
    String responsePackage;
    String htmlData = "";

    if(data != "404") {
      htmlData += "<!DOCTYPE html><html>";
      htmlData += "<head><title>CarCentralServer</title></head>";
      htmlData += "<body>";
      htmlData += "<h1>Data: </h1><br>";
      htmlData += data;
      htmlData += "</body>";
      htmlData += "</html>";

      responsePackage = "HTTP/1.1 200 \r\n";
      responsePackage += "Content-type: text/html \r\n";
      responsePackage += "Connection: close \r\n";       //why???
      responsePackage += "Content-length:" + htmlData.length() + " \r\n";
      responsePackage += "\r\n\r\n"; //end of header
      responsePackage += htmlData;
    } else {
      htmlData += "<!DOCTYPE html><html>";
      htmlData += "<head><title>404 Not Found</title></head>";
      htmlData += "<body>";

      htmlData += "<h1>Not Found </h1><br>";
      htmlData += "<p>" + data + "</p>";

      htmlData += "</body>";
      htmlData += "</html>";

      responsePackage = "HTTP/1.1 404 \r\n";
      responsePackage += "Content-type: text/html \r\n";
      //responsePackage += "Connection: close \r\n";       //why???
      responsePackage += "Content-length:" + htmlData.length() + " \r\n";
      responsePackage += "\r\n\r\n"; //end of header
      responsePackage += htmlData;
  }
    return responsePackage;
  }

  public String generateStringOfData(ArrayList nameOfArray, String nameRequest) {
    String data = "<p>";
    if(nameOfArray.isEmpty()){
      return data + "No data available yet!</p>";
    } else {
      if (nameRequest.contains("Now")) {
        data += nameOfArray.get(nameOfArray.size() - 1);
        data += "</p>";
        return data;
      } else {
        for (int i = nameOfArray.size() - 1; i > -1; i--) {
          data += nameOfArray.get(i) + "<br>";
        }
        data += "</p>";
      }
      return data;
    }
  }

}

