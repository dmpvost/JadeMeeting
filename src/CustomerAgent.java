import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Created by user on 07/01/2016.
 */
public class CustomerAgent extends Agent {

    private boolean leader = false;

    protected void setup() {

        //On aura peut etre besoin de params
       /* Object[] args = getArguments();
        if (args != null && args.length > 0 */


    }

    public boolean isLeader()
    {
        return leader;
    }
    public void setLeader(boolean leader)
    {
        this.leader=leader;
    }

}
