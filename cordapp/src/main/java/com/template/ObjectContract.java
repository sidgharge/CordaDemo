package com.template;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class ObjectContract implements Contract {

    public static final String OBJECT_CONTRACT_ID = "com.template.ObjectContract";

    public interface Commands extends CommandData {

        class Issue extends TypeOnlyCommandData implements Commands{}

        class Transfer extends TypeOnlyCommandData implements Commands{}
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        if (command.getValue() instanceof  Commands.Issue){
            verifyIssue(tx);
        } else if (command.getValue() instanceof Commands.Transfer) {
            verifyTransfer(tx);
        }
        else {
            throw new IllegalArgumentException("UnSupported command...");
        }
    }

    private void verifyIssue(LedgerTransaction tx){
        requireThat(req -> {
           req.using("There should be no input", tx.getInputs().isEmpty());
           req.using("There should be only one output", tx.getOutputs().size() == 1);
           ObjectState state = (ObjectState) tx.getOutput(0);
           //req.using("There should be no lender", state.getLender() == null);
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx){
        requireThat(req -> {
            req.using("There should be only one input", tx.getInputs().size() == 1);
            req.using("There should be only one output", tx.getOutputs().size() == 1);
            ObjectState state = (ObjectState) tx.getOutput(0);
            req.using("Lender should not be borrower", !state.getLender().equals(state.getBorrower()));
            return null;
        });
    }

}
