package CarProducerServer;

import CarProducerServer.TransportService.Iface;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import sun.plugin2.message.transport.Transport;

public class ProducerServer_1 extends Thread {

  public void run() {
    connectionToCarCentralServer();
  }

  public void connectionToCarCentralServer(){
    try {
      // Set port
      TServerSocket serverTransport = new TServerSocket(9898);
      // Set CrawlingHandler we defined before
      // to processor, which handles RPC calls
      // Remember, one service per server
      TransportHandler handler = new TransportHandler();
     TransportService.Processor<TransportService.Iface> processor = new TransportService.Processor<>(handler);

      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
      server.serve();
    } catch (TTransportException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    ProducerServer_1 s1 = new ProducerServer_1();
    s1.start();
    System.out.println("Starting Producer Server 1 ...");
  }

}
