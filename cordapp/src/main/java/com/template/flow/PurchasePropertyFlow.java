package com.template.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contract.PropertyContract;
import com.template.state.Property;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.Nullable;

public class PurchasePropertyFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private String name;

        private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Initialising the transaction...");

        private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building the tx...");

        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the tx...");

        private final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counter-party's signature...") {
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };

        private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising the tx..."){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
        );

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        public Initiator (String name) {
            this.name = name;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(INITIALISING);
            final Property property = new Property(getOurIdentity(), name);

            progressTracker.setCurrentStep(BUILDING);
            final TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(property, PropertyContract.PROPERTY_CONTRACT_ID)
                    .addCommand(new PropertyContract.Commands.Purchase(), getOurIdentity().getOwningKey());

            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction stx = getServiceHub().signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, FINALISING.childProgressTracker()));
        }
    }



    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        @Override
        public Void call() { return null; }
    }
}


