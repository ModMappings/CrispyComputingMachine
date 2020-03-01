package org.modmappings.crispycomputingmachine.readers;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.springframework.batch.item.*;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;

public class MTRespectingReaderAndCompletionPolicy extends SimpleCompletionPolicy implements ItemReader<ExternalVanillaMapping>, ItemStream {

    private final ExternalVanillaMappingReader delegate;

    private ExternalVanillaMapping currentReadItem = null;

    public MTRespectingReaderAndCompletionPolicy(final ExternalVanillaMappingReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public ExternalVanillaMapping read() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
        currentReadItem = delegate.read();
        return currentReadItem;
    }

    @Override
    public RepeatContext start(final RepeatContext context) {
        return new ComparisonPolicyTerminationContext(context);
    }

    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    protected class ComparisonPolicyTerminationContext extends SimpleTerminationContext {

        public ComparisonPolicyTerminationContext(final RepeatContext context) {
            super(context);
        }

        @Override
        public boolean isComplete() {
            final ExternalVanillaMapping nextReadItem;
            try {
                nextReadItem = delegate.peek();
            } catch (Exception e) {
                return true;
            }

            // logic to check if same country
            return nextReadItem == null || currentReadItem.getMappableType() != nextReadItem.getMappableType();
        }
    }
}
