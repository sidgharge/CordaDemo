package com.template;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CordaSerializable
public class ObjectState implements LinearState {

    private AbstractParty lender;

    private AbstractParty borrower;

    private String name;

    private UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public ObjectState(AbstractParty lender, AbstractParty borrower, String name, UniqueIdentifier linearId){
        this.lender = lender;
        this.borrower = borrower;
        this.name = name;
        this.linearId = linearId;
    }

    public ObjectState(AbstractParty lender, AbstractParty borrower, String name){
        this.lender = lender;
        this.borrower = borrower;
        this.name = name;
        this.linearId = new UniqueIdentifier();
    }

    public ObjectState(AbstractParty borrower, String name){
        this.borrower = borrower;
        this.name = name;
        this.linearId = new UniqueIdentifier();
    }

    public AbstractParty getLender() {
        return this.lender;
    }

    public AbstractParty getBorrower() {
        return this.borrower;
    }

    public String getName() {
        return this.name;
    }

    public List<PublicKey> getPublicKeys(){
        List<PublicKey> publicKeys = new ArrayList<>();
        for (AbstractParty party : getParticipants()) {
            publicKeys.add(party.getOwningKey());
        }
        return publicKeys;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> parties = new ArrayList<>();
        parties.add(lender);
        parties.add(borrower);
        return parties;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectState)) {
            return false;
        }
        ObjectState state = (ObjectState) obj;

        return (lender.equals(state.getLender())
                && borrower.equals(state.getBorrower())
                && linearId.equals(state.getLinearId())
                && name.equals(state.getName()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(lender, borrower, name, linearId);
    }
}
