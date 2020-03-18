package org.modmappings.crispycomputingmachine.readers;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class IntermediaryMappingReader extends AbstractItemStreamItemReader<ExternalMapping> implements PeekableItemReader<ExternalMapping>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private final CompositeItemProcessor<String, List<ExternalMapping>> internalVanillaMappingReaderProcessor;

    private PeekingIterator<String> versionIterator = null;
    private String currentVersion = null;
    private PeekingIterator<ExternalMapping> mappingIterator = null;

    public IntermediaryMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> internalIntermediaryMappingReaderProcessor) {
        this.internalVanillaMappingReaderProcessor = internalIntermediaryMappingReaderProcessor;
    }

    @Override
    public void open(final ExecutionContext executionContext) {
        LOGGER.info("Starting the download of the Minecraft Version.");
        super.open(executionContext);
        try {
            final File workingDir = workingDirectory.getFile();
            Assert.state(workingDir.exists(), "Working directory does not exist: " + workingDir.getAbsolutePath());
            Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());
            File metadataFile = new File(workingDir, Constants.INTERMEDIARY_MAVEN_METADATA_FILE);
            Assert.state(metadataFile.exists(), "Intermediary maven metadata file does not exist!");

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

            versionIterator = Iterators.peekingIterator(versions.iterator());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load maven metadata for intermediary mappings.", e);
        }
    }

    @Override
    public ExternalMapping read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Assert.notNull(this.versionIterator, "Missing the version iterator");
        while (this.versionIterator.hasNext() && (this.currentVersion == null || this.mappingIterator == null || !this.mappingIterator.hasNext())) {
            this.currentVersion = this.versionIterator.next();
            LOGGER.info("Loaded: " + this.currentVersion.toString() + " Intermediary mapping version.");

            if (this.currentVersion != null && (this.mappingIterator == null || !this.mappingIterator.hasNext()))
            {
                final List<ExternalMapping> currentVersionMappings = this.internalVanillaMappingReaderProcessor.process(this.currentVersion);
                if (currentVersionMappings != null)
                    this.mappingIterator = Iterators.peekingIterator(Objects.requireNonNull(currentVersionMappings).iterator());
                else
                    this.mappingIterator = null;
            }
        }

        if (this.mappingIterator != null)
        {
            return this.mappingIterator.next();
        }

        LOGGER.info("Loaded all Intermediary mapping versions.");
        return null;
    }

    @Override
    public ExternalMapping peek() throws Exception, UnexpectedInputException, ParseException {
        Assert.notNull(this.versionIterator, "Missing the version iterator");
        while (this.versionIterator.hasNext() && (this.currentVersion == null || this.mappingIterator == null || !this.mappingIterator.hasNext())) {
            this.currentVersion = this.versionIterator.next();
            if (this.currentVersion != null && (this.mappingIterator == null || !this.mappingIterator.hasNext()))
            {
                final List<ExternalMapping> currentVersionMappings = this.internalVanillaMappingReaderProcessor.process(this.currentVersion);
                if (currentVersionMappings != null)
                    this.mappingIterator = Iterators.peekingIterator(Objects.requireNonNull(currentVersionMappings).iterator());
                else
                    this.mappingIterator = null;
            }
        }

        if (this.mappingIterator != null)
        {
            return this.mappingIterator.peek();
        }

        return null;
    }
}
