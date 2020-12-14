package org.modmappings.crispycomputingmachine.readers.policies.completion;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.springframework.batch.item.*;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;

public class MTRespectingReaderAndCompletionPolicy<T extends ExternalMapping> extends SimpleCompletionPolicy implements ItemReader<T>, ItemStream {

    private final PeekableItemReader<T> delegate;

    private T currentReadItem = null;

    public MTRespectingReaderAndCompletionPolicy(final PeekableItemReader<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T read() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
        currentReadItem = delegate.read();
        return currentReadItem;
    }

    @Override
    public RepeatContext start(final RepeatContext context) {
        return new ComparisonPolicyTerminationContext(context);
    }

    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        if (delegate instanceof ItemStream)
            ((ItemStream) delegate).open(executionContext);
    }

    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        if (delegate instanceof ItemStream)
            ((ItemStream) delegate).update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate instanceof ItemStream)
            ((ItemStream) delegate).close();
    }

    protected class ComparisonPolicyTerminationContext extends SimpleTerminationContext {

        public ComparisonPolicyTerminationContext(final RepeatContext context) {
            super(context);
        }

        @Override
        public boolean isComplete() {
            final T nextReadItem;
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
