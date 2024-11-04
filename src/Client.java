import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import ut.Helper;


public class Client {


    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

   /* public static int interpolate(byte[] result1, byte[] result2, int numRows, int numBits) {
        int count = 0;
        for (int i = 0; i < numRows; i++) {
            if ((Math.abs(result1[i] - result2[i])) == numBits) {
                count++;
            }
        }
        return count;
    } */

    public static int interpolate(byte[] result1, byte[] result2, byte[] diff_count, int numRows, int numBits) {
        byte total = 0;
        for (int i = 0; i < numRows; i++) {
            total = (byte) Math.abs(result1[i] - result2[i] - diff_count[i]);
        }
        ;
        return total;
    }

    public static byte[] sendToServer(Object data, ObjectOutputStream outputStream,
                                      ObjectInputStream inputStream) {
        byte[] results = null;
        try {
            // sending data to server
            outputStream.writeObject(data);
            // receiving data from server
            results = (byte[]) inputStream.readObject();
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    public static byte[] readFromServer(ObjectInputStream inputStream) {
        byte[] results = null;
        try {
            // receiving data from server
            results = (byte[]) inputStream.readObject();
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage());
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Class not found when reading results: " + e.getMessage(), e);
        }
        return results;
    }



    /**
     * Return number of rows matching the input query
     * @throws IOException
     */
    public static void processQuery() throws IOException, ExecutionException, InterruptedException{

            // reading properties file for parameters
            Properties config = Helper.readPropertiesFile("config/config.ini");
            int numBits = Integer.parseInt(config.getProperty("num_bits"));
            int numRows = Integer.parseInt(config.getProperty("num_rows"));
            String server1IP = config.getProperty("server1_ip");
            int server1Port = Integer.parseInt(config.getProperty("server1_port"));
            String server2IP = config.getProperty("server2_ip");
            int server2Port = Integer.parseInt(config.getProperty("server2_port"));
            String server3IP = config.getProperty("server3_ip");
            int server3Port = Integer.parseInt(config.getProperty("server3_port"));
            int iter= Integer.parseInt(config.getProperty("iteration"));

            // create sockets for server
            Socket socketServer1 = new Socket(server1IP, server1Port);
            ObjectOutputStream outputStreamServer1 = new ObjectOutputStream(socketServer1.getOutputStream());
            ObjectInputStream inputStreamServer1 = new ObjectInputStream(socketServer1.getInputStream());

            Socket socketServer2 = new Socket(server2IP, server2Port);
            ObjectOutputStream outputStreamServer2 = new ObjectOutputStream(socketServer2.getOutputStream());
            ObjectInputStream inputStreamServer2 = new ObjectInputStream(socketServer2.getInputStream());

            Socket socketServer3 = new Socket(server3IP, server3Port);
            ObjectInputStream inputStreamServer3 = new ObjectInputStream(socketServer3.getInputStream());

            // Ask for query input
            Scanner scanner = new Scanner(System.in);
            ArrayList<Double> totalProcessingTimeList = new ArrayList<>();
            ArrayList<Double> totalNetworkTimeList = new ArrayList<>();
            for (int m = 0; m < iter; m++) {
                System.out.print("Enter your message: ");
                int query = (int) Long.parseLong(scanner.nextLine());
                int query_org = query;

                byte[] result1 = null;
                byte[] result2 = null;
                ArrayList<Instant> processingTime = new ArrayList<>();
                ArrayList<Instant> networkTime = new ArrayList<>();
                ExecutorService executor = Executors.newFixedThreadPool(2);

                @SuppressWarnings("unchecked")
                Future<byte[]>[] futures = (Future<byte[]>[]) new Future<?>[2];

                // Prepare tables T0/T1 with random integers between 0 and 127
                processingTime.add(Instant.now());
                byte[][] T0 = new byte[numBits][2];
                byte[][] T1 = new byte[numBits][2];
                byte[][] T = new byte[numBits][2];
                Random random = new Random();
                int temp = numBits - 1;

                for (int i = temp; i >= 0; i--) {
                    byte bitValue = (byte) (query & 1);
                    T[i][1] = bitValue;
                    T[i][0] = (byte) (1 - bitValue);

                    for (int b = 0; b < 2; b++) {
                        byte currentBit = T[i][b];
                        if (currentBit == 1) {
                            T0[i][b] = (byte) (random.nextInt(100) + 1);
                            T1[i][b] = (byte) (T0[i][b] + 1);
                        } else {
                            int[] z = {0, 100};
                            int randomIndex = random.nextInt(100);
                            T0[i][b] = (byte) (randomIndex == 0 ? z[random.nextInt(z.length)] : randomIndex);
                            T1[i][b] = T0[i][b];
                        }
                    }
                    query >>= 1;
                }
                processingTime.add(Instant.now());

                // Send T0/T1 shares to servers and get responses
                networkTime.add(Instant.now());
                boolean chooseServer = random.nextBoolean();
                if (chooseServer) {
                    futures[0] = executor.submit(() -> sendToServer(T0, outputStreamServer1, inputStreamServer1));
                    futures[1] = executor.submit(() -> sendToServer(T1, outputStreamServer2, inputStreamServer2));
                } else {
                    futures[0] = executor.submit(() -> sendToServer(T0, outputStreamServer2, inputStreamServer2));
                    futures[1] = executor.submit(() -> sendToServer(T1, outputStreamServer1, inputStreamServer1));
                }

                // Wait for responses from Server 1 and Server 2
                result1 = futures[0].get();
                result2 = futures[1].get();

                //from Server 3
                byte[] diff_count = readFromServer(inputStreamServer3);

                networkTime.add(Instant.now());
                processingTime.add(Instant.now());
                System.out.println("The number of items matching query: " + query_org + " is " +
                        interpolate(result1, result2, diff_count, numRows, numBits));
                processingTime.add(Instant.now());

                // Calculate and store processing and network times
                totalProcessingTimeList.add(Helper.getTotalTime(processingTime));
                totalNetworkTimeList.add(Helper.getTotalTime(networkTime));

                executor.shutdown();
            }




        // Calculate Average Processing and Network Time
        double sumProcessingTime = 0; double sumNetworkTime = 0;
        for (double processingTime1 : totalProcessingTimeList) {
            sumProcessingTime += processingTime1;
        }
        for (double networkTime1 : totalNetworkTimeList) {
            sumNetworkTime += networkTime1;
        }

        double averageProcessingTime = sumProcessingTime / iter;
        double averageNetworkTime = sumNetworkTime / iter;
        System.out.println("Average processing time " + averageProcessingTime);
        System.out.println("Temporary network time " + averageNetworkTime);
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        // return number of rows matching the input query
        processQuery();
    }
}
