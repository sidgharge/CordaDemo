package com.template.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.ObjectState;
import com.template.contract.PropertyContract;
import com.template.state.Property;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.List;

public class MovePropertyFlow {


    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier linearId;

        private Party counterparty;

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

        public Initiator (Party counterparty, UniqueIdentifier linearId) {
            this.counterparty = counterparty;
            this.linearId = linearId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            progressTracker.setCurrentStep(INITIALISING);
            final StateAndRef<Property> propertyStateAndRef = getPropertyByLinearId(linearId);
            final Property property = propertyStateAndRef.getState().getData();

            progressTracker.setCurrentStep(BUILDING);
            if (!property.getOwner().equals(getOurIdentity())) {
                throw new FlowException("You must own the property before moving...");
            }

            CommandAndState commandAndState = property.withNewOwner(counterparty);

            List<PublicKey> requiredSigners = ImmutableList.of(counterparty.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addInputState(propertyStateAndRef)
                    .addCommand(new PropertyContract.Commands.Move(), requiredSigners)
                    .addOutputState(commandAndState.getOwnableState(), PropertyContract.PROPERTY_CONTRACT_ID);

            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction stx = getServiceHub().signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(COLLECTING);
            FlowSession session = initiateFlow(counterparty);
            SignedTransaction stx2 = subFlow(new CollectSignaturesFlow(stx,
                    ImmutableList.of(session),
                    ImmutableList.of(getOurIdentity().getOwningKey()),
                    COLLECTING.childProgressTracker()
            ));

            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx2, FINALISING.childProgressTracker()));
        }

        StateAndRef<Property> getPropertyByLinearId(UniqueIdentifier linearId) throws FlowException {
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearId),
                    Vault.StateStatus.UNCONSUMED,
                    null);
            List<StateAndRef<Property>> objects = getServiceHub().getVaultService().queryBy(Property.class, queryCriteria).getStates();
            if (objects.size() != 1){
                throw new FlowException("Property not found...");
            }
            return objects.get(0);
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
