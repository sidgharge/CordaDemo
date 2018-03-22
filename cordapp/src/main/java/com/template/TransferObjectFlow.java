package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import kotlinx.html.COL;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class TransferObjectFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private Party counterparty;

        private UniqueIdentifier linearId;

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

        public Initiator(Party counterparty, UniqueIdentifier linearId){
            this.counterparty = counterparty;
            this.linearId = linearId;
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
            final StateAndRef<ObjectState> objectToTransfer = getObjectStateByLinearId(linearId);
            final ObjectState inputObject = objectToTransfer.getState().getData();

            //Step 2 Building
            progressTracker.setCurrentStep(BUILDING);
            if (inputObject.getBorrower() != getOurIdentity()){
                throw new FlowException("Tranfer must be initiated by the owner");
            }

            ObjectState outputObjectState = new ObjectState(inputObject.getBorrower(), counterparty, inputObject.getName());

            final List<PublicKey> requiredSigners = inputObject.getPublicKeys();

            final TransactionBuilder builder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                    .addInputState(objectToTransfer)
                    .addCommand(new ObjectContract.Commands.Transfer(), requiredSigners)
                    .addOutputState(outputObjectState, ObjectContract.OBJECT_CONTRACT_ID);

            //Step 3 Signing
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder, inputObject.getBorrower().getOwningKey());

            progressTracker.setCurrentStep(COLLECTING);
            final FlowSession session = initiateFlow(counterparty);
            final SignedTransaction signedTransaction2 = subFlow(new CollectSignaturesFlow(
               signedTransaction,
               ImmutableList.of(session),
               ImmutableList.of(getOurIdentity().getOwningKey()),
               COLLECTING.childProgressTracker()
            ));


            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(signedTransaction2, FINALISING.childProgressTracker()));
        }

        StateAndRef<ObjectState> getObjectStateByLinearId(UniqueIdentifier linearId) throws FlowException {
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearId),
                    Vault.StateStatus.UNCONSUMED,
                    null);
            List<StateAndRef<ObjectState>> objects = getServiceHub().getVaultService().queryBy(ObjectState.class, queryCriteria).getStates();
            if (objects.size() != 1){
                throw new FlowException("Object not found...");
            }
            return objects.get(0);
        }
    }

    @InitiatedBy(IssueObjectFlow.Initiator.class)
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
