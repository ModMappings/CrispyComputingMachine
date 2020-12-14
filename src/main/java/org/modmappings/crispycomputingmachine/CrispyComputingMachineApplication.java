package org.modmappings.crispycomputingmachine;

import org.modmappings.crispycomputingmachine.runner.CLIRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@Configuration
public class CrispyComputingMachineApplication {

	public static void main(String[] args) throws Exception
    {
        final List<String> argList = new ArrayList<>(Arrays.asList(args));
        argList.add(0, CrispyComputingMachineApplication.class.getName());

        SpringApplication.run(CrispyComputingMachineApplication.class, argList.toArray(String[]::new));
	}
}
