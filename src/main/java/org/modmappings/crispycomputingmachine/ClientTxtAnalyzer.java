package org.modmappings.crispycomputingmachine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientTxtAnalyzer {

    public static void main(String[] args) throws IOException {
        System.out.println("Reading client.txt for: " + args[0]);
        final List<String> classesInV1 = Files.readAllLines(Paths.get("./working/map_data/" + args[0] + "/client.txt")).parallelStream()
                .filter(l -> !l.startsWith(" "))
                .filter(l -> l.contains(" -> "))
                .map(l -> l.split(" -> ")[0])
                .collect(Collectors.toList());


        System.out.println("Reading client.txt for: " + args[1]);
        final List<String> classesInV2 = Files.readAllLines(Paths.get("./working/map_data/" + args[1] + "/client.txt")).parallelStream()
                .filter(l -> !l.startsWith(" "))
                .filter(l -> l.contains(" -> "))
                .map(l -> l.split(" -> ")[0])
                .collect(Collectors.toList());


        System.out.println("Determining new classes");
        final List<String> newClasses = new ArrayList<>(classesInV2);
        newClasses.removeAll(classesInV1);

        System.out.println("Determining removed classes");
        final List<String> removedClasses = new ArrayList<>(classesInV1);
        removedClasses.removeAll(classesInV2);

        System.out.println("Total classes:");
        System.out.println(" > " + args[0] + ": " + classesInV1.size());
        System.out.println(" > " + args[1] + ": " + classesInV2.size());

        System.out.println();
        System.out.println("New Classes (" + newClasses.size() + "):");
        newClasses.forEach(c -> System.out.println(" > " + c));

        System.out.println();
        System.out.println("Removed Classes (" + removedClasses.size() + "):");
        removedClasses.forEach(c -> System.out.println(" > " + c));

    }
}
