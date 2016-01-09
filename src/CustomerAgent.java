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
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class CustomerAgent extends Agent {

    private boolean leader;
    private Calendar cal;
    private boolean REFUSE = false;
    private boolean done_exec = false;
    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;
    //private int step=0;

    protected void setup() {

        leader = false;
        log(" START");
        cal = new Calendar();


        // ----------- NOT DELETE ---------------------------------
        // ADD ALL AGENTS IN TABLE meetingAgents
        /*AMSAgentDescription [] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults ( new Long(-1) );
            agents = AMSService.search( this, new AMSAgentDescription (), c );
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        for (int i=0; i<agents.length;i++){
            AID meetingAgents = agents[i].getName();
            System.out.println("==>"+meetingAgents.getLocalName());
        }*/
        //---------------------------------------------------------


        //time interval for buyer for sending subsequent CFP
        //as a CLI argument
        int interval = 20000;
        Object[] args = getArguments();
        if (args != null && args.length > 0) interval = Integer.parseInt(args[0].toString());
        addBehaviour(new TickerBehaviour(this, interval) {
            protected void onTick() {
                if (done_exec == false) {
                    // DFAgentDescription
                    DFAgentDescription dfd = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("agent-customer");
                    dfd.addServices(sd);

                    try {
                        SearchConstraints c = new SearchConstraints();
                        c.setMaxResults(new Long(-1));
                        AMSAgentDescription[] agents = AMSService.search(myAgent, new AMSAgentDescription(), c);
                        log(": the following AGENT have been found");

                        // CLEAN THE TABLE OF AGENTS
                        meetingAgents = new AID[agents.length - 3];
                        int i = 0, y = 0;
                        while (i < (agents.length)) {
                            if (!((agents[i].getName().getLocalName().equals("ams")) || (agents[i].getName().getLocalName().equals("df")) || (agents[i].getName().getLocalName().equals("rma")))) {
                                meetingAgents[y] = agents[i].getName();
                                log(meetingAgents[y].getLocalName());
                                y++;
                            }
                            i++;
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    //ADD BEHAVIOUR HERE
                    done_exec = true;
                    log("Add behavior");
                    myAgent.addBehaviour(new masterBehavior());
                }
            }

        });


    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
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

            switch (step) {

                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    log("[INIT]:INIT");
                    // COUNT Number of agent
                    // numberOfAgent=X;
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    // 1. Become the new leader
                    setLeader(true);
                    log("[Leader]:because LEADER");

                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    log("[Leader]:best time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour());

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
                    log("[Leader]: send request MEETING");

                    // 4. switch to waitNewProposal
                    step = 2;

                    log("[Leader]: -> waitNewProposal");
                    setLeader(false);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    if (stepWait == false) {
                        //1. if not the leader,
                        if (leader == false) {
                            // a. wait for the leader agent's date for the meeting
                            log("[waitNewP]:wait the date of the leader");
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
                                    log("[waitNewP]:ACCEPT the date");
                                } else {
                                    //we have to refuse the proposition
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContent("false");
                                    count_disagree++;
                                    REFUSE = true;
                                    log("[waitNewP]:REFUSE the date");
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
                            log("[waitNewP]:Meeting agree. END");
                            step = 4;
                            // END OF DEFINE MEETING ? MAYBE WE STOP HERE ?
                        }//if everybody has responded to the proposal some REFUSE
                        if (count_agree + count_disagree == meetingAgents.length) {
                            //if he is only one to REFUSE: he is the new leader, go to step 1
                            if (count_disagree == 1 && REFUSE == true) {
                                step = 1;
                                leader = true;
                                log("[waitNewP]:Meeting REFUSE : NEW LEADER");
                                //break;
                            }//else some refuse but not the agent, he waits for proposal step2
                            else {
                                step = 2;
                                log("[waitNewP]:Meeting REFUSE : wait new meeting");
                            }
                            //if multiple and the agent said REFUSE, he goes to battle step 3
                            if (count_disagree > 1 && REFUSE == true) {
                                step = 3;
                                log("[waitNewP]:Meeting REFUSE : GO BATTLE");
                            }
                        }
                        break;
                    }
                    // ---------- battleLeader --------------------
                case 3:


                    log("[battleLeader]:Enter in battleLeader randomNumberSend=" + randomNumberSend);
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber();
                        randomNumberSend = true;
                        log("[battleLeader]:Send random number =" + myAleat);
                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    //ACLMessage reply = myAgent.receive(mt);
                    ACLMessage reply = receive();
                    if (reply != null) {
                        log("REPLY ENTER ACL=" + reply.getPerformative() + "  ACL.INFORM=" + ACLMessage.INFORM);
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            //proposal received
                            log("inside REPLY");
                            int getAleat = Integer.parseInt(reply.getContent());
                            log(bestAgent.toString() + " compare" + getAleat + "and " + bestAleat);
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
                                log("[battleLeader]:WIN THE BATTLE with" + myAleat);
                                step = 1;
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                log("[battleLeader]:Loose the battle :" + myAleat);
                                // loose the game go to waitNewProposal
                            }

                            repliesCnt = 0;
                            randomNumberSend = false;
                        }

                    } else {
                        block();
                    }
                    break;
                case 4:
                    log("[END]:END");
                    break;

            }
        }

        public boolean done() {
            log(" --> DONE step=" + step + " randomNumberSend=" + randomNumberSend);
            if (step == 4) {
                return true;
            } else {
                return false;
            }
        }


        public double sendRandomNumber() {
            int aleat = (int) (Math.random() * 10000);

            ACLMessage message = new ACLMessage(ACLMessage.CFP);
            for (int i = 0; i < meetingAgents.length; i++) {
                if (!(meetingAgents[i].getLocalName().equals(myAgent.getLocalName()))) {
                    message.addReceiver(meetingAgents[i]);
                }
            }
            message.setContent(String.valueOf((aleat)));
            message.setPerformative(ACLMessage.INFORM);
            send(message);

            //ACLMessage sendMess = new ACLMessage(ACLMessage.CFP);
            //sendMess.setPerformative(ACLMessage.PROPOSE);
            //sendMess.setContent(String.valueOf((aleat)));
            //myAgent.send(sendMess);
            return aleat;
        }


    }

    public void log(String log) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
        // display time and date using toString()

        System.out.println("[" + ft.format(date) + "][" + getAID().getLocalName() + "]\t" + log);
    }


}
