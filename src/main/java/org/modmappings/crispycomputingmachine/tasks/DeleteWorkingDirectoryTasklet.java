package org.modmappings.crispycomputingmachine.tasks;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;

public class DeleteWorkingDirectoryTasklet implements Tasklet, InitializingBean {

    private Resource workingDirectoryResource;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        File workingDir =  workingDirectoryResource.getFile();

        if (workingDir.exists())
        {
            Assert.state(!workingDir.delete(), "Could not delete the working dir.");
        }

        return RepeatStatus.FINISHED;
    }

    public void setWorkingDirectoryResource(final Resource workingDirectoryResource) {
        this.workingDirectoryResource = workingDirectoryResource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectoryResource, "Working directory is not set.");
    }
}
