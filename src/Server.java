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
     *
     * @param executor          the thread executors
     * @param futures           the thread handler
     * @param rowsPerThread     the number of rows handled by each thread
     * @param remainder         helper variable
     * @param numThreads        the number of threads
     * @param numBits           the number of bits
     * @param serverData        the data stored at the server
     * @param query             the client query
     * @param results           the result formed after server computation
     * @param precomputedPrefix the precomputed prefix values from client share
     * @return the valued computed for each row of server data
     * @throws Exception
     */
    public static byte[] processQueryWithPrecompute(ExecutorService executor, Future<?>[] futures,
                                                    int rowsPerThread, int remainder, int numThreads,
                                                    int numBits, long[] serverData, byte[][] query,
                                                    byte[] results, byte[] remvalues, short[] precomputedPrefix) throws Exception {

        // running threads
        for (int t = 0; t < numThreads; t++) {
            final int start = t * rowsPerThread + Math.min(t, remainder);
            final int end = start + rowsPerThread + (t < remainder ? 1 : 0);

            futures[t] = executor.submit(() -> {
                short result;
                long data, data_org;
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
                    remvalues[i] = (byte) (result % div);
                    results[i] = (byte) (result / div);
                   ;
                    //For mod Count:
                    /*byte[] sum;
                    remvalues[i] = (byte) (result % div);
                    results[i] = (byte) ((result+q)/div);
                    */
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


    public static void main(String[] args) throws IOException {
        // reading server number
        if (args.length != 1) {
            System.out.println("Please provide the server number (1 or 2)");
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
        int iter = Integer.parseInt(config.getProperty("iteration"));


        // reading server data
        long[] serverData = Helper.loadDataValues(dataPath, numBits, numRows);

        ArrayList<Double> totalServerTimeList = new ArrayList<>();

        try {
            System.out.println("Server" + serverNumber + " Listening........");

            byte[][] query;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            byte[] results;
            byte[] remValues;


                System.out.println("Loaded " + serverData.length + " rows of data");
                ServerSocket ss = new ServerSocket(serverPort);
                Socket socketServer = ss.accept();
                System.out.println("Connected ");
                ObjectInputStream inputStream = new ObjectInputStream(socketServer.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketServer.getOutputStream());
                String serverIP3 = config.getProperty("server3_ip");
                int serverPort3 = Integer.parseInt(config.getProperty("server3_port"));
                Socket socketToServer3 = new Socket(serverIP3, serverPort3);
                ObjectOutputStream outputStreamToServer3 = new ObjectOutputStream(socketToServer3.getOutputStream());

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
                    // Sending to server 3
                    outputStreamToServer3.writeObject(remValues);
                    outputStreamToServer3.close();
                    System.out.println("Sent remainder values to Server 3");
                    //Sending to Client
                    objectOutputStream.writeObject(results);

                }
                executor.shutdown();

        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        //Calculate Average
        double sumServerTime = 0;
        for (double servertime : totalServerTimeList) {
            sumServerTime += servertime;
        }
        System.out.println("Average Server time " + sumServerTime / iter);

    }
}