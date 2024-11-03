import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import ut.Helper;

public class Server3 {

    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ArrayList<Double> totalServerTimeList = new ArrayList<>();
        int serverNumber = 3;
        Properties config = Helper.readPropertiesFile("config/config.ini");
        int numRows = Integer.parseInt(config.getProperty("num_rows"));
        int serverPort = Integer.parseInt(config.getProperty("server" + serverNumber + "_port"));
        String clientIP = config.getProperty("client_ip");
        int clientPort = Integer.parseInt(config.getProperty("client_port"));

        try {
            System.out.println("Server" + serverNumber + " Listening........");
            ServerSocket ss = new ServerSocket(serverPort);
            Socket socketServer = ss.accept();
            System.out.println("Connected to Client");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketServer.getOutputStream());

            // Create connections to Server1 and Server2
            Socket socketServer1 = new Socket(config.getProperty("server1_ip"), Integer.parseInt(config.getProperty("server1_port")));
            Socket socketServer2 = new Socket(config.getProperty("server2_ip"), Integer.parseInt(config.getProperty("server2_port")));
            ObjectInputStream inputStream1 = new ObjectInputStream(socketServer1.getInputStream());
            ObjectInputStream inputStream2 = new ObjectInputStream(socketServer2.getInputStream());

            // Wait for data from both servers
            byte[] fromServer1 = (byte[]) inputStream1.readObject();
            byte[] fromServer2 = (byte[]) inputStream2.readObject();

            // Process the received data
            byte[] compare = new byte[numRows];
            ArrayList<Instant> processingTime = new ArrayList<>();
            processingTime.add(Instant.now());

            for (int j = 0; j < numRows; j++) {
                compare[j] = (byte) (Math.abs(fromServer1[j]) > Math.abs(fromServer2[j]) ? 1 : 0);
            }
            // Send the result back to the client
            objectOutputStream.writeObject(compare);

            processingTime.add(Instant.now());
            totalServerTimeList.add(Helper.getTotalTime(processingTime));

            // Close connections
            inputStream1.close();
            inputStream2.close();
            objectOutputStream.close();
            socketServer.close();
            socketServer1.close();
            socketServer2.close();

        } catch (IOException | ClassNotFoundException ex) {
            log.log(Level.SEVERE, "Error in Server 3", ex);
        } finally {
            // Calculate Average
            double sumServerTime = totalServerTimeList.stream().mapToDouble(Double::doubleValue).sum();
            System.out.println("Average Server time " + (sumServerTime / totalServerTimeList.size()));
        }
    }
}
