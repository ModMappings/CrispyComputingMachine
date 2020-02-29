package org.modmappings.crispycomputingmachine.tasks;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;

@Component
public class DeleteWorkingDirectoryTasklet implements Tasklet, InitializingBean {

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        File workingDir =  workingDirectory.getFile();

        if (workingDir.exists())
        {
            Assert.state(!workingDir.delete(), "Could not delete the working dir.");
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectory, "The working directory is not set.");
    }
}
