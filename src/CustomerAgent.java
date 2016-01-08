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
    private boolean REFUSE = false;

    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;

    protected void setup() {
        leader = false;

        System.out.println("Agent " + getAID().getName() + " START.");

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
        System.out.println("Agent " + getAID().getName() + " terminated.");
    }


    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }


    private class masterBehavior extends Behaviour {
        private int step = 0;
        private MessageTemplate messTemplate;
        private int numberOfAgent = 0;
        private int count_disagree = 0;
        private int count_agree = 0;

        //schedule
        private String day;
        private String hour;
        private boolean stepWait = false;

        // battle
        private int counter = -1;
        private int bestAleat = -1;
        private int repliesCnt = 0;
        private AID bestAgent;

        private double myAleat = 0;
        private boolean randomNumberSend = false;

        public void action() {

            // init
            step = 0;

            switch (step) {

                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    System.out.println(getAID().getName() + "\t[INIT]:\tINIT");
                    // COUNT Number of agent
                    // numberOfAgent=X;
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    // 1. Become the new leader
                    setLeader(true);
                    System.out.println(getAID().getName() + "\t[Leader]:\tbecause LEADER");

                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    System.out.println(getAID().getName() + "\t[Leader]:\tbest time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour());

                    // 3. Send this date as CFP
                    //call for proposal (CFP) to found agents
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < meetingAgents.length; ++i) {
                        cfp.addReceiver(meetingAgents[i]);
                    }

                    String proposal_date = meeting.getDay() + "-" + meeting.getHour();
                    cfp.setContent(proposal_date);
                    cfp.setPerformative(ACLMessage.PROPOSE); //???? we need to put this or not ?
                    cfp.setConversationId("meeting date");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value

                    myAgent.send(cfp);
                    messTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(proposal_date),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    System.out.println(getAID().getName() + "\t[Leader]\t: send request MEETING");

                    // 4. switch to waitNewProposal
                    step = 2;

                    System.out.println(getAID().getName() + "\t[Leader]\t: -> waitNewProposal");
                    setLeader(false);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    if (stepWait == false) {
                        //1. if not the leader,
                        if (leader == false) {
                            // a. wait for the leader agent's date for the meeting
                            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                            ACLMessage msg = myAgent.receive(mt);
                            if (msg != null) {
                                String date = msg.getContent();
                                ACLMessage reply = msg.createReply();
                                //find if the hour and the day is possible for the agent:
                                String[] parts = date.split("-");
                                day = parts[0];
                                hour = parts[1];
                                double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                                if (possibility != 0) {
                                    //We can accept the proposition and return true
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContent("true");
                                    count_agree++;
                                } else {
                                    //we have to refuse the proposition
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContent("false");
                                    count_disagree++;
                                    REFUSE = true;
                                }
                                stepWait = true;
                                myAgent.send(reply);

                            } else {
                                block();
                            }
                        }
                    } else if (stepWait == true) {
                        //for everybody: receive the responses
                        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        ACLMessage msg = myAgent.receive(mt);
                        if (msg != null) {
                            if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                                count_agree++;
                            if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL)
                                count_disagree++;
                        }//if everybody has responded to the proposal, all ACCEPT
                        if (count_agree == meetingAgents.length) {
                            //we fix the schedule
                            cal.putMeetingInDate(Integer.parseInt(day), Integer.parseInt(hour));
                            //go to step 3 battle to know the next leader

                            step = 3;
                            // END OF DEFINE MEETING ? MAYBE WE STOP HERE ?
                        }//if everybody has responded to the proposal some REFUSE
                        if (count_agree + count_disagree == meetingAgents.length) {
                            //if he is only one to REFUSE: he is the new leader, go to step 1
                            if (count_disagree == 1 && REFUSE == true) {
                                step = 1;
                                leader = true;
                                //break;
                            }//else some refuse but not the agent, he waits for proposal step2
                            else {
                                step = 2;
                            }
                            //if multiple and the agent said REFUSE, he goes to battle step 3
                            if (count_disagree > 1 && REFUSE == true) {
                                step = 3;
                            }
                        }
                        break;
                    }
                    // ---------- battleLeader --------------------
                case 3:


                    System.out.println(getAID().getName() + "\t[battleLeader]:\tEnter in battleLeader");
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber();
                        randomNumberSend = false;
                        System.out.println(getAID().getName() + "\t[battleLeader]:\tSend random number =" + myAleat);
                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPAGATE) {
                            //proposal received
                            int getAleat = Integer.parseInt(reply.getContent());

                            if (bestAgent == null || getAleat < bestAleat) {
                                //the best proposal as for now
                                bestAleat = getAleat;
                                bestAgent = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= meetingAgents.length) {
                            //all proposals have been received
                            if (myAleat > bestAleat) {
                                System.out.println(getAID().getName() + "\t[battleLeader]:\tWIN THE BATTLE with" + myAleat);
                                step = 1;
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                System.out.println(getAID().getName() + "\t[battleLeader]:\tLoose the battle :" + myAleat);
                                // loose the game go to waitNewProposal
                            }

                            repliesCnt = 0;
                            randomNumberSend = false;
                        }

                    } else {
                        block();
                    }
                    break;

            }
        }

        public boolean done() {
            return true;
        }


        public double sendRandomNumber() {
            double aleat = Math.random();
            ACLMessage sendMess = new ACLMessage(ACLMessage.CFP);
            sendMess.setPerformative(ACLMessage.PROPAGATE);
            sendMess.setContent(String.valueOf((aleat)));
            myAgent.send(sendMess);
            return aleat;
        }


    }


}
