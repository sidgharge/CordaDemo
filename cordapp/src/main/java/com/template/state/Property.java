package com.template.state;

import com.google.common.collect.ImmutableList;
import com.template.contract.PropertyContract;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.OwnableState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Property implements OwnableState, LinearState {

    private AbstractParty owner;

    private String name;

    private UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public Property(AbstractParty owner, String name, UniqueIdentifier linearId) {
        this.owner = owner;
        this.name = name;
        this.linearId = linearId;
    }

    public Property(AbstractParty owner, String name) {
        this.owner = owner;
        this.name = name;
        this.linearId = new UniqueIdentifier();
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public AbstractParty getOwner() {
        return this.owner;
    }

    @NotNull
    @Override
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        Property property = new Property(newOwner, name, linearId);
        return new CommandAndState(new PropertyContract.Commands.Move(), property);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(owner);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
