package org.modmappings.crispycomputingmachine.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class CRONRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CRONRunner.class);

    private JobLauncher jobLauncher;

    private JobExplorer jobExplorer;

    private JobRepository jobRepository;

    private JobBuilderFactory jobBuilderFactory;


}
