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

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class CustomerAgent extends Agent {

    private boolean leader;
    private Calendar cal;

    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;

    protected void setup() {
        leader = false ;
        cal = new Calendar();


        //On aura peut etre besoin de params
       /* Object[] args = getArguments();
        if (args != null && args.length > 0 */
    }
    protected void takeDown() {
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
    }

    public boolean isLeader()
    {
        return leader;
    }
    public void setLeader(boolean leader)
    {
        this.leader=leader;
    }


    private class leaderBehavior extends Behaviour {
        private MessageTemplate messTemplate;
        public void action() {

            // 1. Become the new leader
            setLeader(true);

            // 2. Look for the best date to propose a meeting
            Hour meeting = cal.getBestHour();

            // 3. Send this date as CFP
            //call for proposal (CFP) to found sellers
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (int i = 0; i < meetingAgents.length; ++i) {
                cfp.addReceiver(meetingAgents[i]);
            }

            String proposal_date = meeting.getDay()+"-"+meeting.getHour();
            cfp.setContent(proposal_date);
            cfp.setConversationId(proposal_date);
            cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value

            myAgent.send(cfp);
            messTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(proposal_date),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
        }
        public boolean done() {
            return true;
        }
    }

    private class waitNewProposalBehavior extends Behaviour {
        public void action() {}
        public boolean done() {
            return true;
        }
    }

    private class battleLeaderBehavior extends Behaviour {
        public void action() {}
        public boolean done() {
            return true;
        }
    }

}
