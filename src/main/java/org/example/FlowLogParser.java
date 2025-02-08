package org.example;

import java.io.*;
import java.util.*;

public class FlowLogParser {
    public static void main(String[] args) {

//        System.out.println("Current Working Directory: " + new File(".").getAbsolutePath());
        String flowLogFile = "flow_logs.txt";
        String lookupFile = "lookup_table.csv";
        String outputFile = "output_report.txt";

        try {
            Map<String, Set<String>> lookupMap = loadLookupTable(lookupFile);
            processFlowLogs(flowLogFile, lookupMap, outputFile);
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private static Map<String, Set<String>> loadLookupTable(String lookupFile) throws IOException {
        Map<String, Set<String>> lookupMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(lookupFile))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String key = parts[0].trim() + "," + parts[1].trim().toLowerCase();
                    String tag = parts[2].trim();

                    lookupMap.putIfAbsent(key, new HashSet<>());
                    lookupMap.get(key).add(tag);
                }
            }
        }
        return lookupMap;
    }

    private static void processFlowLogs(String flowLogFile, Map<String, Set<String>> lookupMap, String outputFile) throws IOException {
        Map<String, Integer> tagCounts = new HashMap<>();
        Map<String, Integer> portProtocolCounts = new HashMap<>();
        int untaggedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(flowLogFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length < 13) continue;

                String dstPort = parts[5].trim();
                String protocol = getProtocolName(parts[7].trim()).toLowerCase();
                String key = dstPort + "," + protocol;

                Set<String> tags = lookupMap.getOrDefault(key, new HashSet<>());
                if (tags.isEmpty()) {
                    untaggedCount++;
                } else {
                    for (String tag : tags) {
                        tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
                    }
                }

                portProtocolCounts.put(key, portProtocolCounts.getOrDefault(key, 0) + 1);
            }
        }

        writeOutputFile(outputFile, tagCounts, portProtocolCounts, untaggedCount);
    }

    private static String getProtocolName(String protocolNumber) {
        switch (protocolNumber) {
            case "6": return "tcp";
            case "17": return "udp";
            case "1": return "icmp";
            default: return "unknown";
        }
    }

    private static void writeOutputFile(String outputFile, Map<String, Integer> tagCounts,
                                        Map<String, Integer> portProtocolCounts, int untaggedCount) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Tag Counts:\n");
            writer.write("Tag,Count\n");
            for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            writer.write("Untagged," + untaggedCount + "\n\n");

            writer.write("Port/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");
            for (Map.Entry<String, Integer> entry : portProtocolCounts.entrySet()) {
                writer.write(entry.getKey().replace(",", ",") + "," + entry.getValue() + "\n");
            }
        }
        System.out.println("Output written to: " + outputFile);
    }
}
