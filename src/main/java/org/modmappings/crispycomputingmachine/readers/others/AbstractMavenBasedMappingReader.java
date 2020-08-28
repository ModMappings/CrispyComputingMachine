package org.modmappings.crispycomputingmachine.readers.others;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractMavenBasedMappingReader extends AbstractItemStreamItemReader<ExternalMapping> implements PeekableItemReader<ExternalMapping> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final CompositeItemProcessor<String, List<ExternalMapping>> processor;

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private String currentRelease = null;
    private PeekingIterator<String> availableReleasesIterator = null;
    private PeekingIterator<ExternalMapping> mappingIterator = null;

    private final String mappingTypeName;
    private final String metadataFilePath;

    public AbstractMavenBasedMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> processor, final String mappingTypeName, final String metadataFilePath) {
        this.processor = processor;
        this.mappingTypeName = mappingTypeName;
        this.metadataFilePath = metadataFilePath;
    }

    @Override
    public void open(final ExecutionContext executionContext) {
        LOGGER.info(String.format("Starting the download of the manifest for: %s.", mappingTypeName));
        super.open(executionContext);
        try {
            final File workingDir = workingDirectory.getFile();
            Assert.state(workingDir.exists(), "Working directory does not exist: " + workingDir.getAbsolutePath());
            Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());

            File metadataFile = new File(workingDir, metadataFilePath);
            Assert.state(metadataFile.exists(), String.format("%s maven metadata file does not exist!", mappingTypeName));

            FileInputStream fileIS = new FileInputStream(metadataFile);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(fileIS);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/metadata/versioning/versions/version";
            final NodeList versionLists = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            final List<String> versions = IntStream.range(0, versionLists.getLength())
                    .mapToObj(versionLists::item)
                    .map(Node::getTextContent)
                    .collect(Collectors.toList());

            availableReleasesIterator = Iterators.peekingIterator(versions.iterator());
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to load maven metadata for %s mappings.", mappingTypeName), e);
        }
    }

    @Override
    public ExternalMapping read() throws Exception {
        return this.processMappingRequest(PeekingIterator::next);
    }

    @Override
    public ExternalMapping peek() throws Exception {
        return this.processMappingRequest(PeekingIterator::peek);
    }

    private ExternalMapping processMappingRequest(Function<PeekingIterator<ExternalMapping>, ExternalMapping> iteratorInvoker) throws Exception {
        Assert.notNull(this.availableReleasesIterator, "Missing the version iterator");
        while (this.availableReleasesIterator.hasNext() && (this.currentRelease == null || this.mappingIterator == null || !this.mappingIterator.hasNext())) {
            this.currentRelease = this.availableReleasesIterator.next();
            if (this.currentRelease != null && (this.mappingIterator == null || !this.mappingIterator.hasNext()))
            {
                final List<ExternalMapping> currentVersionMappings = this.processor.process(this.currentRelease);
                if (currentVersionMappings != null)
                    this.mappingIterator = Iterators.peekingIterator(Objects.requireNonNull(currentVersionMappings).iterator());
                else
                    this.mappingIterator = null;
            }
        }

        if (this.mappingIterator != null)
        {
            return iteratorInvoker.apply(this.mappingIterator);
        }
        return null;
    }
}
