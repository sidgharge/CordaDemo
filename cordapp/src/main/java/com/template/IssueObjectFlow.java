package com.template;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class IssueObjectFlow {

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

        public Initiator(String name){
            this.name = name;
        }

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //Step 1 Initialisation
            progressTracker.setCurrentStep(INITIALISING);
            final ObjectState objectState = new ObjectState(getOurIdentity(), this.name);
            List<PublicKey> requiredSigners = new ArrayList<>();
            requiredSigners.add(getOurIdentity().getOwningKey());

            //Step 2 Building
            progressTracker.setCurrentStep(BUILDING);
            //final List<PublicKey> requiredSigners = objectState.getPublicKeys();

            final TransactionBuilder builder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addOutputState(objectState, ObjectContract.OBJECT_CONTRACT_ID)
                    .addCommand(new ObjectContract.Commands.Issue(), requiredSigners);

            //Step 3 Signing
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder, objectState.getBorrower().getOwningKey());

            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(signedTransaction, FINALISING.childProgressTracker()));
        }
    }

}
