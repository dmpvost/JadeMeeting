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
        this.addBehaviour(new leaderBehavior());
        this.addBehaviour(new waitNewProposalBehavior());
        this.addBehaviour(new battleLeaderBehavior());
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

        public void action() {
            switch (step) {

                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    // COUNT Number of agent
                    // numberOfAgent=X;
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
                    cfp.setPerformative(ACLMessage.PROPOSE);
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
                            }
                            myAgent.send(reply);
                        } else {
                            block();
                        }
                    }//if it's the leader, wait
                    else {

                    }

                    //2. COUNT number of TRUE AND FALSE (totalAgent -1, because leader propose)

                    //collect proposals

                    /*ACLMessage reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)
                        {
                            //proposal received
                            int price = Integer.parseInt(reply.getContent());
                            if ((bestSeller == null || price < bestPrice))
                            {
                                //the best proposal as for now
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            //all proposals have been received
                            step = 2;
                            if(bestPrice>total_budget)
                            {
                                step=4;
                                targetBookTitle="";
                                count_time=0;
                                System.out.println("buyer : No book find on my budget");
                            }
                        }

                    }
                    else
                    {
                        block(150);
                        step=1;
                        count_time++;
                        System.out.println("DEBUG  count_time:"+count_time);
                        if(count_time>5)
                        {
                            step=4;
                            count_time=0;
                            System.out.println("No answer, end of request");
                            targetBookTitle = "";
                        }
                    }*/

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

                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)
                        {
                            //proposal received
                            int price = Integer.parseInt(reply.getContent());
                            if ((bestSeller == null || price < bestPrice))
                            {
                                //the best proposal as for now
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            //all proposals have been received
                            step = 2;
                            if(bestPrice>total_budget)
                            {
                                step=4;
                                targetBookTitle="";
                                count_time=0;
                                System.out.println("buyer : No book find on my budget");
                            }
                        }

                    }
                    else
                    {
                        block(150);
                        step=1;
                        count_time++;
                        System.out.println("DEBUG  count_time:"+count_time);
                        if(count_time>5)
                        {
                            step=4;
                            count_time=0;
                            System.out.println("No answer, end of request");
                            targetBookTitle = "";
                        }
                    }



                    break;

            }
        }

        public boolean done() {
            return true;
        }
    }

//SWITCH CASSSEE///////////////////////////
    private class leaderBehavior extends Behaviour {


        public void action() {


        }

        public boolean done() {
            return true;
        }
    }

    private class waitNewProposalBehavior extends Behaviour {
        public void action() {



        }

        public boolean done() {
            return true;
        }
    }

    private class battleLeaderBehavior extends Behaviour {
        public void action() {
        }

        public boolean done() {
            return true;
        }
    }

}
