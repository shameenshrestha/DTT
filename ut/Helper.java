package ut;

import java.io.*;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Helper {
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final int buffer = 10;
    private static final int runCount = 20;
    private static final int phaseCount = 3;
    private static double[][] totalNetworkTime = new double[phaseCount][runCount];
    private static double[][] totalProcessTime = new double[phaseCount][runCount];

    private static long[][] resultLong;
    private static BigInteger[][] resultBigint;

    public static double getTotalTime(ArrayList<Instant> timestamps) {
        ArrayList<Duration> durations = new ArrayList<>();

        for (int i = 0; i < timestamps.size() - 1; i = i + 2) {
            durations.add(Duration.between(timestamps.get(i), timestamps.get(i + 1)));
        }
        return durations.stream().mapToDouble(Duration::toNanos).sum() / Math.pow(10, 6);
    }

    public static Properties readPropertiesFile(String fileName) {
        FileInputStream fileInputStream;
        Properties properties = null;
        try {
            fileInputStream = new FileInputStream(fileName);
            properties = new Properties();
            properties.load(fileInputStream);
        } catch (IOException ioException) {
            log.log(Level.SEVERE, ioException.getMessage());
        }
        return properties;
    }

    public static void display(byte[][] data) {
        for (byte[] i : data) {
            for (byte j : i) {
                System.out.print(j + ",");
            }
            System.out.println();
        }


//        for (int i=0;i<2;i++) {
//            for (int j=0;j<4;j++) {
//                System.out.print(data[j][i] + ",");
//            }
//            System.out.println();
//        }
    }

    public static long[] loadDataValues(String filePath, int numBits, int numRows) {
        long[] valuesBinary = new long[numRows];

        try (BufferedReader file = new BufferedReader(new FileReader(filePath))) {
            String line;

            int rowIndex = 0;
            while ((line = file.readLine()) != null && rowIndex < numRows) {
                String[] values = line.split(",");
                if (values.length > 0) {
                    // this try/catch block is because some csv files have the header, while others do not
                    try {
                        long dataValue = Long.parseLong(values[0].trim());
                        valuesBinary[rowIndex] = dataValue;
                        ++rowIndex;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Unable to open file at path: " + filePath);
            return null;
        }

        return valuesBinary;
    }

    public static int[] getLeadingZeros(String filePath, int numBits, int numRows) {
        int[] valuesBinary = new int[numRows];

        try (BufferedReader file = new BufferedReader(new FileReader(filePath))) {
            String line;

            int rowIndex = 0;
            int temp1 = 64 - numBits;
            while ((line = file.readLine()) != null && rowIndex < numRows) {
                String[] values = line.split(",");
                if (values.length > 0) {
                    // this try/catch block is because some csv files have the header, while others do not
                    try {
                        long dataValue = Long.parseLong(values[0].trim());
                        valuesBinary[rowIndex] = Long.numberOfLeadingZeros(dataValue) - temp1;
                        ;
                        ++rowIndex;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Unable to open file at path: " + filePath);
            return null;
        }

        return valuesBinary;
    }


//    public static int getRunCount() {
//        return runCount;
//    }
//
//    public static int getBuffer() {
//        return buffer;
//    }


//    public static long mod(long number) {
//        return Math.floorMod(number, Constant.getModParameter());
//    }
//
//    public static int mod(int number) {
//        return (int) Math.floorMod(number, Constant.getModParameter());
//    }
//
//
//    public static BigInteger mod(BigInteger number) {
//        return number.mod(Constant.getModBigParameter());
//    }
//
//    /**
//     * To read the file data as string values
//     *
//     * @param file the file to be read
//     * @return
//     * @throws IOException
//     */
//    public static BigInteger[][] readFileAsBigInt(File file) throws IOException {
//
//        // to store all lines of the file
//        ArrayList<String> lines = new ArrayList<>();
//
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//
//        // reading all lines
//        while (line != null) {
//            lines.add(line);
//            line = bf.readLine();
//        }
//
//        // to store file into array
//        BigInteger[][] result = new BigInteger[lines.size()][];
//        int i = 0;
//        for (String l : lines) {
//            result[i] = Arrays.stream(l.split(",")).map(BigInteger::new).toArray(BigInteger[]::new);
//            i++;
//        }
//
//        return result;
//    }
//
//
//    /**
//     * To read the file data as long values
//     *
//     * @param file the file to be read
//     * @return
//     * @throws IOException
//     */
//    public static long[][] readFileAsLong(File file) throws IOException {
//
//        // to store all lines of the file
//        ArrayList<String> lines = new ArrayList<>();
//
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//
//        // reading all lines
//        while (line != null) {
//            lines.add(line);
//            line = bf.readLine();
//        }
//
//        // to store file into array
//        long[][] result = new long[lines.size()][];
//        int i = 0;
//        for (String l : lines) {
//            result[i] = Arrays.stream(l.split(",")).mapToLong(Long::parseLong).toArray();
//            i++;
//        }
//
//        return result;
//
//
//    }
//
//    public static long[] readFileAs1D(File file) throws IOException {
//        // to store all lines of the file
//        ArrayList<String> lines = new ArrayList<>();
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//        // reading all lines
//        while (line != null) {
//            lines.add(line);
//            line = bf.readLine();
//        }
//        // to store file into array
//        List<Long> temp = new ArrayList<>();
//        for (String l : lines) {
//            temp.addAll(Arrays.stream(l.split(",")).map(Long::valueOf).toList());
//        }
//        long[] result;
//        result = temp.stream().mapToLong(Long::longValue).toArray();
////        System.out.println("optinv sue:"+result.length);
//        return result;
//    }
//
//    /**
//     * To read the file data as long values
//     *
//     * @param file the file to be read
//     * @return
//     * @throws IOException
//     */
//    public static long[][] readFileAsLong1(File file, int serverNumber) throws IOException {
//
//        // to store all lines of the file
//        ArrayList<String> lines = new ArrayList<>();
//
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//
//        // reading all lines
//        while (line != null) {
//            lines.add(line);
//            line = bf.readLine();
//        }
//
//        // to store file into array
//        long[][] result = new long[lines.size() + 1][];
//        int i = 0;
//        for (String l : lines) {
//            result[i] = Arrays.stream(l.split(",")).mapToLong(Long::parseLong).toArray();
//            i++;
//        }
//        result[result.length - 1] = new long[]{serverNumber};
//        return result;
//    }
//
//    /**
//     * To read the file data as long values
//     *
//     * @param file the file to be read
//     * @return
//     * @throws IOException
//     */
//    public static void readFileAsLongBigint(File file) throws IOException {
//
//        // to store all lines of the file
//        ArrayList<String> lines = new ArrayList<>();
//
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//
//        // reading all lines
//        while (line != null) {
//            lines.add(line);
//            line = bf.readLine();
//        }
//
//        // to store file into array
//        String[] temp;
//        resultLong = new long[lines.size()][2];
//        resultBigint = new BigInteger[lines.size()][2];
//        int i = 0;
//        for (String l : lines) {
//            temp = l.split(",");
//            resultLong[i][0] = Long.parseLong(temp[0]);
//            resultLong[i][1] = Long.parseLong(temp[1]);
//            resultBigint[i][0] = new BigInteger(temp[2]);
//            resultBigint[i][1] = new BigInteger(temp[3]);
//            i++;
//        }
//    }
//
//    public static String[] readFile(String fileName) throws IOException {
//        File file = new File(fileName);
//        ArrayList<String> lines = new ArrayList<>();
//        BufferedReader bf = new BufferedReader(new FileReader(file));
//        String line = bf.readLine();
//        while (line != null) {
//            lines.add(line.replace("[", ""));
//            line = bf.readLine();
//        }
//        // to store file into array
//        String[] result = lines.get(0).split(",");
//        return result;
//    }
//
//    public static long[][] getResultLong() {
//        return resultLong;
//    }
//
//    public static BigInteger[][] getResultBigint() {
//        return resultBigint;
//    }
//
//
//    public static void writeToFile(String fileName, String content, String phase) {
//        try {
//            FileWriter myWriter = new FileWriter("results/Phase" + phase + "/Doc_" + fileName + ".txt");
//            myWriter.write(content);
//            myWriter.close();
//        } catch (IOException e) {
//            System.out.println("An error occurred while writing to the file.");
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * To flush the contents of "results" folder
//     */
//    public static void cleanResultFolder() {
//        File folder = new File("results/Phase2/");
//        String[] files = folder.list();
//        // iterate over each file within the folder
//        for (String s : files) {
//            File currentFile = new File(folder.getPath(), s);
//            currentFile.delete();
//        }
//
//        folder = new File("results/Phase3/");
//        files = folder.list();
//        // iterate over each file within the folder
//        for (String s : files) {
//            File currentFile = new File(folder.getPath(), s);
//            currentFile.delete();
//        }
//    }
//
//    public static Double getTotalTime(ArrayList<Instant> timestamps) {
//        ArrayList<Duration> durations = new ArrayList<>();
//
//        for (int i = 0; i < timestamps.size() - 1; i = i + 2) {
//            durations.add(Duration.between(timestamps.get(i), timestamps.get(i + 1)));
//        }
//        return (durations.stream().mapToDouble(Duration::toMillis).sum());
//    }
//
//    public static double calculateSendToServerTime(ArrayList<Instant> sendToServerTime,
//                                                   ArrayList<Instant> sendToServerWaitTime, int serverCount) {
//
//        double networkTime, waitingTime;
//        waitingTime = getTotalTime(sendToServerWaitTime);
//        networkTime = getTotalTime(sendToServerTime) - waitingTime;
//
//        return networkTime / serverCount;
//
//    }
//
//
//    public static void printTimesNew(String op, ArrayList<Instant> waitTime,
//                                     ArrayList<Instant> comTime, ArrayList<Instant> removeTime,
//                                     ArrayList<Double> perServerTime, double totalTime, int count) {
//        double processingTime, networkTime, waitingTime;
//
//        if (removeTime == null) {
//            waitingTime = getTotalTime(waitTime);
//            networkTime = getTotalTime(comTime) - waitingTime;
//            processingTime = totalTime - getTotalTime(comTime);
//        } else {
//
//            networkTime = perServerTime.stream()
//                    .mapToDouble(a -> a)
//                    .sum();
//
//            processingTime = totalTime - getTotalTime(removeTime);
//        }
//
//        System.out.println("Processing time taken by " + op + " operation  is " + processingTime + " ms");
//        System.out.println("Network time taken by " + op + " operation  is " + networkTime + " ms");
//
//        totalNetworkTime[0][count] = networkTime;
//        totalProcessTime[0][count] = processingTime;
//    }
//
//    public static void printTimes(int phase, ArrayList<Instant> waitTime,
//                                  ArrayList<Instant> comTime, ArrayList<Instant> removeTime,
//                                  ArrayList<Double> perServerTime, double totalTime, int count) {
//        double processingTime = 0.0, networkTime = 0.0, waitingTime;
//
//        if (removeTime == null) {
//            System.out.println("Need to omit");
////            waitingTime = getTotalTime(waitTime);
////            networkTime = getTotalTime(comTime) - waitingTime;
////            processingTime = totalTime - getTotalTime(comTime);
//        } else {
//            networkTime = perServerTime.stream()
//                    .mapToDouble(a -> a)
//                    .sum();
//            processingTime = totalTime - getTotalTime(removeTime);
//        }
//
//        System.out.println("Processing time taken by Phase" + phase + " is " + processingTime + " ms");
//        System.out.println("Network time taken by Phase" + phase + " is " + networkTime + " ms");
//        phase = phase - 1;
//
//        totalNetworkTime[phase][count] = networkTime;
//        totalProcessTime[phase][count] = processingTime;
//    }
//
//    public static double calculateStandardDeviation(double[] array) {
//
////        for(double a:array){
////            System.out.print(a+" ");
////        }
////        System.out.println();
//
//        // get the sum of array
//        double sum = 0.0;
//        for (double i : array) {
//            sum += i;
//        }
//
//        // get the mean of array
//        int length = array.length;
//        double mean = sum / length;
//
//        // calculate the standard deviation
//        double standardDeviation = 0.0;
//        for (double num : array) {
//            standardDeviation += Math.pow(num - mean, 2);
//        }
//
//        return Math.sqrt(standardDeviation / length);
//    }
//
//    public static void timeTaken(int type) {
//
//
//
//        double div = runCount - buffer;
//        String str = "";
//        if (type == 0)
//            str = "Client";
//        else
//            str = "Server" + type;
//
//        System.out.println("Time taken by " + str + ":");
//        for (int j = 0; j < phaseCount; j++) {
//            long processingTime = 0, networkTime = 0;
//            double[] totalTime = new double[runCount - buffer];
//            System.out.println("PHASE:" + (j + 1));
//            System.out.println("==================================================================");
//            for (int i = buffer; i < runCount; i++) {
//                networkTime += totalNetworkTime[j][i];
//                processingTime += totalProcessTime[j][i];
////                totalTime[i - buffer] =  totalProcessTime[j][i];
//                totalTime[i - buffer] = totalNetworkTime[j][i] + totalProcessTime[j][i];
//            }
//
//
//            System.out.println("Processing time taken by Phase" + (j + 1) + " is " + (processingTime / div) + " ms");
//            System.out.println("Network time taken by Phase" + (j + 1) + " is " + (networkTime / div) + " ms");
//            System.out.println("Total time taken by Phase" + (j + 1) + " is " + ((networkTime + processingTime) / div) + " ms");
//            System.out.println(calculateStandardDeviation(totalTime));
//        }
//
//
//        totalNetworkTime = new double[phaseCount][runCount];
//        totalProcessTime = new double[phaseCount][runCount];
//    }
//
//    public static void display(LinkedHashMap<String, Map<String, String>> map) {
//        for (String key : map.keySet()) {
//            String value = map.get(key).toString();
//            System.out.println(key + " " + value);
//        }
//    }

}
