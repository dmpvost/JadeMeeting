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
    private boolean REFUSE= false;

    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;

    protected void setup() {
        leader = false;
        cal = new Calendar();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent-customer");
        sd.setName("JADE-meeting-scheduler");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //ADD BEHAVIOUR HERE
        this.addBehaviour(new masterBehavior());
    }

    protected void takeDown() {
        //book selling service deregistration at DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Seller agent " + getAID().getName() + " terminated.");
    }


    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }


    private class masterBehavior extends Behaviour {
        private int step = 0 ;
        private MessageTemplate messTemplate;
        private int numberOfAgent=0;
        private int count_disagree=0;
        private int count_agree=0;

        // battle
        private int counter=-1;
        private int bestAleat=-1;
        private int repliesCnt = 0;
        private AID bestAgent;

        public void action() {

            // init
            step=0;

            switch (step) {

                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    // COUNT Number of agent
                    // numberOfAgent=X;
                    step=3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    // 1. Become the new leader
                    setLeader(true);

                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();

                    // 3. Send this date as CFP
                    //call for proposal (CFP) to found agents
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < meetingAgents.length; ++i) {
                        cfp.addReceiver(meetingAgents[i]);
                    }

                    String proposal_date = meeting.getDay() + "-" + meeting.getHour();
                    cfp.setContent(proposal_date);
                    //cfp.setPerformative(ACLMessage.PROPOSE); ???? we need to put this or not ?
                    cfp.setConversationId("meeting date");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value

                    myAgent.send(cfp);
                    messTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(proposal_date),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    // 4. switch to waitNewProposal
                    step=2;
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    //1. if not the leader
                    if (leader == false) {
                        // a. wait for the leader agent's date for the meeting
                        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        ACLMessage msg = myAgent.receive(mt);
                        if (msg != null) {
                            String date = msg.getContent();
                            ACLMessage reply = msg.createReply();
                            //find if the hour and the day is possible for the agent:
                            String[] parts = date.split("-");
                            String day = parts[0];
                            String hour = parts[1];
                            double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                            if (possibility != 0) {
                                //We can accept the proposition and return true
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContent("true");
                                count_agree++;
                            } else {
                                //we have to refuse the proposition
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("false");
                                count_disagree++;
                                REFUSE = true;
                            }
                            myAgent.send(reply);
                        } else {
                            block();
                        }
                    }//for everybody: receive the accepts or not
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
                            count_agree++;
                        }else{
                            count_disagree++;
                        }
                    }//if everybody has responded to the proposal, all ACCEPT
                    if(count_agree == meetingAgents.length){
                        //go to step 3 battle to know the next leader
                        step

                    }//if everybody has responded to the proposal some REFUSE
                    if(count_agree + count_disagree == meetingAgents.length){
                        //if he is only one to REFUSE: he is the new leader, go to step 1
                        if(count_disagree == 1 && REFUSE == true){
                            step = 1;
                            leader = true;
                            break;
                        }else{
                            step = 2;
                            break;
                        }
                        //if multiple and the agent said REFUSE, he goes to battle step 3
                        if(count_disagree > 1 && REFUSE == true){
                            step = 3;
                        }
                    }
                    break;

                // ---------- battleLeader --------------------
                case 3:
                    /*
                    BattleLeaderBehavior
                    1.Collect number or FALSE -> if equal 1
                        => become LeaderBehavior
                        IF > 2 ,  send/share random number.
                    2.An agent collect all random number, compare his to the rest, if his number is the biggest he becomes the leader.
                        Bigger number -> LeaderBehavior
                        The others -> WaitNewProposalBehavior
                     */

                    //1. IF count_agree = 1 -> become Leader
                    if ( count_disagree==1)
                        step=1;
                        // if the agent are in the BATTLE and count_disagree=1, it's mean that he is alone, so he become Leader
                    else {

                        double myAleat = sendRandomNumber();

                        //collect proposals
                        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null)
                        {
                            if (reply.getPerformative() == ACLMessage.PROPAGATE)
                            {
                                //proposal received
                                int getAleat = Integer.parseInt(reply.getContent());

                                if (bestAgent == null || getAleat < bestAleat)
                                {
                                    //the best proposal as for now
                                    bestAleat = getAleat;
                                    bestAgent = reply.getSender();
                                }
                            }
                            repliesCnt++;
                            if (repliesCnt >= meetingAgents.length) {
                                //all proposals have been received
                                if (myAleat > bestAleat) {
                                    System.out.println(getAID().getLocalName() + " become the Leader !");
                                    step=1;
                                    // WIN THE BATTLE -> become leader
                                } else {
                                    step=2;
                                    // loose the game go to waitNewProposal
                                }
                            }

                        } else {
                            block();
                        }

                    }

                    break;

            }
        }

        public boolean done() {
            return true;
        }



        public double sendRandomNumber()
        {
            double aleat = Math.random();
            ACLMessage sendMess = new ACLMessage( ACLMessage.CFP);
            sendMess.setPerformative(ACLMessage.PROPAGATE);
            sendMess.setContent(String.valueOf((aleat)));
            myAgent.send(sendMess);
            return aleat;
        }




    }



}
