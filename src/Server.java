import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import ut.Helper;

public class Server {

    public static int div = 8;
    public static int q = 136;

    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * To process the client query using pre-computation approach
     * @param executor the thread executors
     * @param futures the thread handler
     * @param rowsPerThread the number of rows handled by each thread
     * @param remainder helper variable
     * @param numThreads the number of threads
     * @param numBits the number of bits
     * @param serverData the data stored at the server
     * @param query the client query
     * @param results the result formed after server computation
     * @param precomputedPrefix the precomputed prefix values from client share
     * @return the valued computed for each row of server data
     * @throws Exception
     */
    public static byte[] processQueryWithPrecompute(ExecutorService executor, Future<?>[] futures,
                                                    int rowsPerThread, int remainder, int numThreads,
                                                    int numBits, long[] serverData, byte[][] query,
                                                    byte[] results,byte[] remvalues, short[] precomputedPrefix) throws Exception {

        // running threads
        for (int t = 0; t < numThreads; t++) {
            final int start = t * rowsPerThread + Math.min(t, remainder);
            final int end = start + rowsPerThread + (t < remainder ? 1 : 0);

            futures[t] = executor.submit(() -> {
                short result;
                long data,data_org;
                int temp = numBits - 1;
                byte mod = 127;
                int j, leadingZeros, temp1 = 64 - numBits;
                byte bitValue;
                for (int i = start; i < end; i++) {
                    data = serverData[i];
                    data_org = data;
                    leadingZeros = Long.numberOfLeadingZeros(data) - temp1;
                    result = (precomputedPrefix[leadingZeros]);
                    for (j = temp; j >= leadingZeros; j--) {
                        bitValue = (byte) (data & 1);
                        result += (query[j][bitValue]);
                        data >>= 1;
                    }
                    results[i] = (byte) (result/div);
                    remvalues[i] = (byte) (result % div);
                    //For mod Count:
                    /*byte[] sum;
                    sum[i]=(byte) ((result%mod);
                    results[i] = (byte) ((result+q)/div);*/
                    serverData[i] = data_org;
                }
            });
        }
        // waiting for threads to complete
        for (Future<?> future : futures) {
            future.get();
        }

        return results;
    }

    public static byte[] processCountQuery(ExecutorService executor, Future<?>[] futures,
                                           int rowsPerThread, int remainder, int numThreads,
                                           int numBits, long[] serverData, byte[][] query,
                                           byte[] remValues, short[] precomputedPrefix) throws Exception {


        for (int t = 0; t < numThreads; t++) {
            final int start = t * rowsPerThread + Math.min(t, remainder);
            final int end = start + rowsPerThread + (t < remainder ? 1 : 0);

            futures[t] = executor.submit(() -> {
                short result;
                long data, data_org;
                short currentresult;
                int temp = numBits - 1;
                byte mod = 127;
                int j,  temp1 = 64 - numBits;
                byte bitValue;

                for (int i = start; i < end; i++) {
                    data = serverData[i];
                    data_org = data;
                    for (j = temp; j >= 0; j--) {
                        bitValue = (byte) (data & 1);
                        currentresult = query[j][bitValue];
                        //FOR Mod Count:
                        remValues[i] = (byte) (currentresult % mod);
                        data >>= 1;
                    }
                    serverData[i] = data_org;
                }

            });
        }
        for (Future<?> future : futures) {
            future.get();
        }
        return remValues;
    }





    public static void main(String[] args) throws IOException {
        // reading server number
        if (args.length != 1) {
            System.out.println("Please provide the server number (1 or 2 or 3)");
            return;
        }
        int serverNumber = Integer.parseInt(args[0]);
        System.out.println("Server Number: " + serverNumber); // Debugging line


        Properties config = Helper.readPropertiesFile("config/config.ini");
        int numBits = Integer.parseInt(config.getProperty("num_bits"));
        int numThreads = Integer.parseInt(config.getProperty("num_threads"));
        int numRows = Integer.parseInt(config.getProperty("num_rows"));
        String serverIP = config.getProperty("server" + serverNumber + "_ip");
        int serverPort = Integer.parseInt(config.getProperty("server" + serverNumber + "_port"));
        int clientPort = Integer.parseInt(config.getProperty("client_port"));
        String clientIP = config.getProperty("client_ip");
        String dataPath = config.getProperty("csv_file_path");
        int iter= Integer.parseInt(config.getProperty("iteration"));

        // reading server data


        long[] serverData = Helper.loadDataValues(dataPath, numBits, numRows);

        ArrayList<Double> totalServerTimeList = new ArrayList<>();

        try {
            System.out.println("Server" + serverNumber + " Listening........");

            byte[][] query;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            byte[] results;
            byte[] remValues;

            if (serverNumber==1 || serverNumber==2) {
                System.out.println("Loaded " + serverData.length + " rows of data");
                ServerSocket ss = new ServerSocket(serverPort);
                Socket socketServer = ss.accept();
                System.out.println("Connected ");
                ObjectInputStream inputStream = new ObjectInputStream(socketServer.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketServer.getOutputStream());

                for (int i = 0; i < 10; i++) {
                    // initialization
                    ArrayList<Instant> processingTime = new ArrayList<>();
                    results = new byte[numRows];
                    remValues = new byte[numRows];
                    int rowsPerThread = numRows / numThreads;
                    int remainder = numRows % numThreads;
                    Future<?>[] futures = new Future<?>[numThreads];
                    short[] precomputedPrefix = new short[numBits + 1];

                    query = (byte[][]) inputStream.readObject();
                    processingTime.add(Instant.now());
                    //without pre-computation
                    // processQueryWithoutPrecompute(executor, futures, rowsPerThread, remainder, numThreads, numBits,
                    //   serverData, query, results, remValues);

                    // with pre-computation
                    precomputedPrefix[1] = query[0][0];
                    for (int m = 1; m < numBits; m++) {
                        precomputedPrefix[m + 1] = (short) (precomputedPrefix[m] + (query[m][0]));
                    }

                    results = processQueryWithPrecompute(executor, futures, rowsPerThread, remainder, numThreads, numBits,
                            serverData, query, results, remValues, precomputedPrefix);

                    processingTime.add(Instant.now());
                    System.out.println("Processing time:" + Helper.getTotalTime(processingTime));
                    totalServerTimeList.add(Helper.getTotalTime(processingTime));

                    try {
                        String serverIP3 = config.getProperty("server3_ip");
                        int serverPort3 = Integer.parseInt(config.getProperty("server3_port"));
                        Socket socketToServer3 = new Socket(serverIP3, serverPort3);
                        ObjectOutputStream outputStreamToServer3 = new ObjectOutputStream(socketToServer3.getOutputStream());
                        outputStreamToServer3.writeObject(remValues);

                        outputStreamToServer3.close();
                        socketToServer3.close();
                        System.out.println("Sent remainder values to Server 3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // sending result
                    objectOutputStream.writeObject(results);
                    objectOutputStream.writeObject(remValues);


                }
                executor.shutdown();
            }

            if (serverNumber == 3) {
                System.out.println("Server 3: Comparing the Remainders");

                ServerSocket ss = new ServerSocket(serverPort);
                Socket clientSocket = ss.accept();
                System.out.println("Connected to Client");

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                // ExecutorService executor = Executors.newFixedThreadPool(2);


                Future<byte[]> server1Future = executor.submit(() -> {
                    Socket socketServer1 = ss.accept();
                    System.out.println("Connected to Server 1");
                    ObjectInputStream inputStream1 = new ObjectInputStream(socketServer1.getInputStream());
                    return (byte[]) inputStream1.readObject();
                });

                Future<byte[]> server2Future = executor.submit(() -> {
                    Socket socketServer2 = ss.accept();
                    System.out.println("Connected to Server 2");
                    ObjectInputStream inputStream2 = new ObjectInputStream(socketServer2.getInputStream());
                    return (byte[]) inputStream2.readObject();
                });

                // Retrieve the results from both servers
                for (int i = 0; i < 10; i++) {
                    byte[] rem_Server1 = server1Future.get();  // Get result from Server 1
                    byte[] rem_Server2 = server2Future.get();  // Get result from Server 2

                    // Compare the remainders
                    byte[] comp = new byte[numRows];
                    ArrayList<Instant> processingTime = new ArrayList<>();
                    processingTime.add(Instant.now());

                    for (int j = 0; j < numRows; j++) {
                        if (Math.abs(rem_Server1[j]) > Math.abs(rem_Server2[j])) {
                            comp[j] = 1;  // Server 1 remainder is greater
                        } else {
                            comp[j] = 0;  // Server 2 remainder is greater or equal
                        }
                    }

                    // Send the comparison result back to the client
                    objectOutputStream.writeObject(comp);

                    // End measuring processing time
                    processingTime.add(Instant.now());
                    System.out.println("Processing time: " + Helper.getTotalTime(processingTime));
                    totalServerTimeList.add(Helper.getTotalTime(processingTime));
                }

                executor.shutdown();
            }


        } catch (IOException | ClassNotFoundException ex) {
            log.log(Level.SEVERE, ex.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Calculate Average
        double sumServerTime = 0;
        for (double servertime : totalServerTimeList) {
            sumServerTime += servertime;
        }
        double averageServerTime = sumServerTime / iter;
        System.out.println("Average Server time " + averageServerTime);

    }
}