package CarProducerServer;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

public class ProducerServer_2 extends Thread{

  public void run(){
    connectionToProducerServer_1();
  }

  public void connectionToProducerServer_1(){
    try {
      // Set port
      TServerSocket serverTransport = new TServerSocket(9899);
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
    ProducerServer_2 s2 = new ProducerServer_2();
    s2.start();
    System.out.println("Starting Producer Server 2 ...");
  }
}
