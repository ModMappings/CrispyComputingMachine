package org.modmappings.crispycomputingmachine.tasks;

import net.minecraftforge.lex.mappingtoy.Utils;
import org.modmappings.crispycomputingmachine.utils.Constants;
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
public class DownloadMinecraftManifestTasklet implements Tasklet, InitializingBean {

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        File workingDir =  workingDirectory.getFile();

        if (!workingDir.exists())
        {
            Assert.state(workingDir.mkdirs(), "Could not create the working directory: " + workingDir.getAbsolutePath());
        }
        Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());

        File manifestFile = new File(workingDir, Constants.MANIFEST_WORKING_FILE);
        if (manifestFile.exists())
            Assert.state(manifestFile.delete(), "Failed to delete the manifest file. It might be in use!");

        Utils.downloadFileEtag(manifestFile.toPath(), Constants.MANIFEST_DOWNLOAD_URL);
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectory, "The working directory is not set.");
    }
}
