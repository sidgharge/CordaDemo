package com.template.contract;

import com.template.state.Property;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class PropertyContract implements Contract {

    public static final String PROPERTY_CONTRACT_ID = "com.template.contract.PropertyContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        final Commands commandData = command.getValue();

        if (commandData instanceof Commands.Purchase) {
            verifyPurchase(tx);
        } else if (commandData instanceof  Commands.Move) {
            verifyMove(tx);
        }
    }

    private void verifyMove(LedgerTransaction tx) {
        requireThat(req -> {
            List<Property> propertyInputStates = tx.inputsOfType(Property.class);
            req.using("There must be only one input state...", propertyInputStates.size() == 1);

            List<Property> propertyOutputSTates = tx.outputsOfType(Property.class);
            req.using("There must be only one output state...", propertyOutputSTates.size() == 1);

            req.using("Buyer and seller must be two different parties...", !propertyInputStates.get(0).getOwner().equals(propertyOutputSTates.get(0).getOwner()));

            return null;
        });
    }

    private void verifyPurchase(LedgerTransaction tx) {
        requireThat(req -> {
            List<Property> propertyInputStates = tx.inputsOfType(Property.class);
            req.using("There must be no input state...", propertyInputStates.size() == 0);

            List<Property> propertyOutputSTates = tx.outputsOfType(Property.class);
            req.using("There must be only one output state...", propertyOutputSTates.size() == 1);

            return null;
        });
    }

    public interface Commands extends CommandData {

        class Purchase extends TypeOnlyCommandData implements Commands{}

        class Move extends TypeOnlyCommandData implements Commands{}

    }
}
