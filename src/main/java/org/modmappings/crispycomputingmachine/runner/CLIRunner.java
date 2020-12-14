package org.modmappings.crispycomputingmachine.runner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.*;
import org.springframework.batch.core.launch.support.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class CLIRunner implements CommandLineRunner {

    private static final Log LOGGER = LogFactory.getLog(CommandLineJobRunner.class);
    private static final List<String> VALID_OPTS = Arrays.asList("-restart", "-next", "-stop", "-abandon");

    private static SystemExiter systemExiter = new JvmSystemExiter();
    private static String message = "";

    private ConfigurableApplicationContext context = null;
    private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();
    private JobLauncher launcher;
    private JobLocator jobLocator;

    // Package private for unit test
    private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();
    private JobExplorer jobExplorer;
    private JobRepository jobRepository;

    public CLIRunner(
      final ConfigurableApplicationContext context,
      final JobLauncher launcher,
      final JobLocator jobLocator,
      final JobExplorer jobExplorer,
      final JobRepository jobRepository)
    {
        this.context = context;
        this.launcher = launcher;
        this.jobLocator = jobLocator;
        this.jobExplorer = jobExplorer;
        this.jobRepository = jobRepository;
    }

    public ConfigurableApplicationContext getContext()
    {
        return context;
    }

    public void setContext(final ConfigurableApplicationContext context)
    {
        this.context = context;
    }

    public ExitCodeMapper getExitCodeMapper()
    {
        return exitCodeMapper;
    }

    public void setExitCodeMapper(final ExitCodeMapper exitCodeMapper)
    {
        this.exitCodeMapper = exitCodeMapper;
    }

    public JobLauncher getLauncher()
    {
        return launcher;
    }

    public void setLauncher(final JobLauncher launcher)
    {
        this.launcher = launcher;
    }

    public JobLocator getJobLocator()
    {
        return jobLocator;
    }

    public void setJobLocator(final JobLocator jobLocator)
    {
        this.jobLocator = jobLocator;
    }

    public JobParametersConverter getJobParametersConverter()
    {
        return jobParametersConverter;
    }

    public void setJobParametersConverter(final JobParametersConverter jobParametersConverter)
    {
        this.jobParametersConverter = jobParametersConverter;
    }

    public JobExplorer getJobExplorer()
    {
        return jobExplorer;
    }

    public void setJobExplorer(final JobExplorer jobExplorer)
    {
        this.jobExplorer = jobExplorer;
    }

    public JobRepository getJobRepository()
    {
        return jobRepository;
    }

    public void setJobRepository(final JobRepository jobRepository)
    {
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(final String... args) throws Exception
    {
        List<String> newargs = new ArrayList<>(Arrays.asList(args));

        try {
            if (System.in.available() > 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String line = " ";
                while (line != null) {
                    if (!line.startsWith("#") && StringUtils.hasText(line)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Stdin arg: " + line);
                        }
                        newargs.add(line);
                    }
                    line = reader.readLine();
                }
            }
        }
        catch (IOException e) {
            LOGGER.warn("Could not access stdin (maybe a platform limitation)");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception details", e);
            }
        }

        Set<String> opts = new LinkedHashSet<>();
        List<String> params = new ArrayList<>();

        int count = 0;
        String jobPath = null;
        String jobIdentifier = null;

        for (String arg : newargs) {
            if (VALID_OPTS.contains(arg)) {
                opts.add(arg);
            }
            else {
                switch (count) {
                    case 0:
                        jobPath = arg;
                        break;
                    case 1:
                        jobIdentifier = arg;
                        break;
                    default:
                        params.add(arg);
                        break;
                }
                count++;
            }
        }

        if (jobPath == null || jobIdentifier == null) {
            String message = "At least 2 arguments are required: JobPath/JobClass and jobIdentifier.";
            LOGGER.error(message);
            CLIRunner.message = message;
            exit(1);
            return;
        }

        String[] parameters = params.toArray(new String[params.size()]);

        int result = start(jobPath, jobIdentifier, parameters, opts);
        exit(result);
    }

    public void exit(int status) {
        systemExiter.exit(status);
    }

    int start(String jobPath, String jobIdentifier, String[] parameters, Set<String> opts) {
        try {
            Assert.state(launcher != null, "A JobLauncher must be provided.  Please add one to the configuration.");
            if (opts.contains("-restart") || opts.contains("-next")) {
                Assert.state(jobExplorer != null,
                  "A JobExplorer must be provided for a restart or start next operation.  Please add one to the configuration.");
            }

            String jobName = jobIdentifier;

            JobParameters jobParameters = jobParametersConverter.getJobParameters(StringUtils
                                                                                    .splitArrayElementsIntoProperties(parameters, "="));
            Assert.isTrue(parameters.length == 0 || !jobParameters.isEmpty(),
              "Invalid JobParameters " + Arrays.asList(parameters)
                + ". If parameters are provided they should be in the form name=value (no whitespace).");

            if (opts.contains("-stop")) {
                List<JobExecution> jobExecutions = getRunningJobExecutions(jobIdentifier);
                if (jobExecutions == null) {
                    throw new JobExecutionNotRunningException("No running execution found for job=" + jobIdentifier);
                }
                for (JobExecution jobExecution : jobExecutions) {
                    jobExecution.setStatus(BatchStatus.STOPPING);
                    jobRepository.update(jobExecution);
                }
                return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
            }

            if (opts.contains("-abandon")) {
                List<JobExecution> jobExecutions = getStoppedJobExecutions(jobIdentifier);
                if (jobExecutions == null) {
                    throw new JobExecutionNotStoppedException("No stopped execution found for job=" + jobIdentifier);
                }
                for (JobExecution jobExecution : jobExecutions) {
                    jobExecution.setStatus(BatchStatus.ABANDONED);
                    jobRepository.update(jobExecution);
                }
                return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
            }

            if (opts.contains("-restart")) {
                JobExecution jobExecution = getLastFailedJobExecution(jobIdentifier);
                if (jobExecution == null) {
                    throw new JobExecutionNotFailedException("No failed or stopped execution found for job="
                                                               + jobIdentifier);
                }
                jobParameters = jobExecution.getJobParameters();
                jobName = jobExecution.getJobInstance().getJobName();
            }

            Job job = null;
            if (jobLocator != null) {
                try {
                    job = jobLocator.getJob(jobName);
                } catch (NoSuchJobException ignored) {
                }
            }
            if (job == null) {
                job = (Job) context.getBean(jobName);
            }

            if (opts.contains("-next")) {
                jobParameters = new JobParametersBuilder(jobParameters, jobExplorer)
                                  .getNextJobParameters(job)
                                  .toJobParameters();
            }

            JobExecution jobExecution = launcher.run(job, jobParameters);
            return exitCodeMapper.intValue(jobExecution.getExitStatus().getExitCode());

        }
        catch (Throwable e) {
            String message = "Job Terminated in error: " + e.getMessage();
            LOGGER.error(message, e);
            CLIRunner.message = message;
            return exitCodeMapper.intValue(ExitStatus.FAILED.getExitCode());
        }
        finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /**
     * @param jobIdentifier a job execution id or job name
     * @param minStatus the highest status to exclude from the result
     * @return
     */
    private List<JobExecution> getJobExecutionsWithStatusGreaterThan(String jobIdentifier, BatchStatus minStatus) {

        Long executionId = getLongIdentifier(jobIdentifier);
        if (executionId != null) {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            if (jobExecution.getStatus().isGreaterThan(minStatus)) {
                return Arrays.asList(jobExecution);
            }
            return Collections.emptyList();
        }

        int start = 0;
        int count = 100;
        List<JobExecution> executions = new ArrayList<>();
        List<JobInstance> lastInstances = jobExplorer.getJobInstances(jobIdentifier, start, count);

        while (!lastInstances.isEmpty()) {

            for (JobInstance jobInstance : lastInstances) {
                List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                if (jobExecutions == null || jobExecutions.isEmpty()) {
                    continue;
                }
                for (JobExecution jobExecution : jobExecutions) {
                    if (jobExecution.getStatus().isGreaterThan(minStatus)) {
                        executions.add(jobExecution);
                    }
                }
            }

            start += count;
            lastInstances = jobExplorer.getJobInstances(jobIdentifier, start, count);

        }

        return executions;

    }

    private JobExecution getLastFailedJobExecution(String jobIdentifier) {
        List<JobExecution> jobExecutions = getJobExecutionsWithStatusGreaterThan(jobIdentifier, BatchStatus.STOPPING);
        if (jobExecutions.isEmpty()) {
            return null;
        }
        return jobExecutions.get(0);
    }

    private List<JobExecution> getStoppedJobExecutions(String jobIdentifier) {
        List<JobExecution> jobExecutions = getJobExecutionsWithStatusGreaterThan(jobIdentifier, BatchStatus.STARTED);
        if (jobExecutions.isEmpty()) {
            return null;
        }
        List<JobExecution> result = new ArrayList<>();
        for (JobExecution jobExecution : jobExecutions) {
            if (jobExecution.getStatus() != BatchStatus.ABANDONED) {
                result.add(jobExecution);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private List<JobExecution> getRunningJobExecutions(String jobIdentifier) {
        Long executionId = getLongIdentifier(jobIdentifier);
        List<JobExecution> result = new ArrayList<>();
        if (executionId != null) {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            if (jobExecution != null && jobExecution.isRunning()) {
                result.add(jobExecution);
            }
        }
        else {
            result.addAll(jobExplorer.findRunningJobExecutions(jobIdentifier));
        }
        return result.isEmpty() ? null : result;
    }

    private Long getLongIdentifier(String jobIdentifier) {
        try {
            return new Long(jobIdentifier);
        }
        catch (NumberFormatException e) {
            // Not an ID - must be a name
            return null;
        }
    }
}
