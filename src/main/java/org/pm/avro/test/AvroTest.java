package org.pm.avro.test;

import example.avro.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author pmaresca
 */
public class AvroTest {

    private static int[] PAYLOAD_SIZES = {64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};

    private static final int TESTS = 1000;
    private static final int REPETITIONS = 5;
    
    private static final int RESCALE = 1000;

    private static final boolean DEBUG = false;

    public static void main(String args[]) 
    {
        Map<Integer, Double> timesSummary = new HashMap<Integer, Double>();
        Map<Integer, Double> sizesSummary = new HashMap<Integer, Double>();
        double[] times = new double[TESTS];
        double[] sizes = new double[TESTS];
        
        int cumulativeSize = 0;
        
        long startBenchmark = System.currentTimeMillis();
        try {
            AvroSerializer.instance().setUp();
            
            for(int s = 0; s < PAYLOAD_SIZES.length; s++) {
                for (int t = 0; t < TESTS; t++) {
                    long startTest = System.nanoTime();
                    for (int r = 0; r < REPETITIONS; r++) {
                        Collection<Message> messages = createMessages(1, PAYLOAD_SIZES[s]);

                        Iterator<Message> itr = messages.iterator();
                        while (itr.hasNext()) {
                            Message orgMsg = itr.next();
                            if (DEBUG) 
                                System.out.println("Original:\n   " +orgMsg);
                            byte[] msgBytes = AvroSerializer.instance().serialize(orgMsg);
                            cumulativeSize += msgBytes.length;
                            Message rebMsg = (Message) AvroSerializer.instance().deserialize(msgBytes);
                            if (DEBUG) 
                                System.out.println("Rebuilt:\n   " +rebMsg);
                        }
                    }
                    times[t] = ((System.nanoTime() - startTest) / REPETITIONS);
                    sizes[t] = cumulativeSize / REPETITIONS;
                    cumulativeSize = 0;
                }
                timesSummary.put(PAYLOAD_SIZES[s], StatUtils.mean(times));
                sizesSummary.put(PAYLOAD_SIZES[s], StatUtils.mean(sizes));
            }

            AvroSerializer.instance().tearDown();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        printSummary(timesSummary, sizesSummary, 
                        (System.currentTimeMillis() - startBenchmark));
    }

    private static Collection<Message> createMessages(int nr, int size) 
    {
        Collection<Message> messages = new LinkedList<>();

        for (int i = 0; i < nr; i++) {
            String payload = RandomStringUtils.randomAlphanumeric(size);
            Message msg = Message.newBuilder()
                    .setHeader(Long.toString(System.nanoTime()))
                    .setPayload(payload)
                    .build();
            
            messages.add(msg);
        }

        return messages;
    }
    
    private static void printSummary(Map<Integer, Double> times,
                                        Map<Integer, Double> sizes, 
                                            long timeSpent) 
    {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("Benchamrk Summary - [");
        sBuilder.append(Long.toString(timeSpent));
        sBuilder.append("ms]\n");
        sBuilder.append("{");
        sBuilder.append("\n  #tests: ");
        sBuilder.append(Integer.toString(TESTS));
        sBuilder.append("\n  #repetitions: ");
        sBuilder.append(Integer.toString(REPETITIONS));
        sBuilder.append("\n");
        for(Integer key: times.keySet()) {
            sBuilder.append("\n\n  ");
            sBuilder.append(key.toString() +"bytes payload:");
            sBuilder.append("\n       mean[us]: ");
            sBuilder.append(Double.toString(times.get(key) / RESCALE ) );
            sBuilder.append("\n    size[bytes]: ");
            sBuilder.append(Double.toString(sizes.get(key)) );
        }
        sBuilder.append("\n}");
        
        System.out.println(sBuilder.toString());
    }

}
