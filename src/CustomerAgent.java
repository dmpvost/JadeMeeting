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

public class CustomerAgent extends Agent {

    private boolean leader;
    private Calendar cal;
    private boolean REFUSE = false;

    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;

    protected void setup() {

        leader = false;
        System.out.println("Agent " + getAID().getLocalName() + " START.");
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
        addBehaviour(new TickerBehaviour(this, interval)
        {
            protected void onTick()
            {
                // DFAgentDescription
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("agent-customer");
                dfd.addServices(sd);

                try
                {   SearchConstraints c = new SearchConstraints();
                    c.setMaxResults ( new Long(-1) );
                    AMSAgentDescription [] agents = AMSService.search( myAgent, new AMSAgentDescription (), c );
                    //DFAgentDescription[] result  = DFService.search(myAgent, dfd);
                    System.out.println(": the following AGENT have been found");
                    meetingAgents = new AID[agents.length];
                    for (int i = 0; i < agents.length; ++i)
                    {
                        meetingAgents[i] = agents[i].getName();
                        System.out.println(meetingAgents[i].getLocalName());
                    }
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                //ADD BEHAVIOUR HERE
                myAgent.addBehaviour(new masterBehavior());
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

            // init
            step = 3;

            switch (step) {

                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    log("\t[INIT]:\tINIT");
                    // COUNT Number of agent
                    // numberOfAgent=X;
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    // 1. Become the new leader
                    setLeader(true);
                    log("\t[Leader]:\tbecause LEADER");

                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    log("\t[Leader]:\tbest time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour());

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
                    log("\t[Leader]\t: send request MEETING");

                    // 4. switch to waitNewProposal
                    step = 2;

                    log("\t[Leader]\t: -> waitNewProposal");
                    setLeader(false);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    if (stepWait == false) {
                        //1. if not the leader,
                        if (leader == false) {
                            // a. wait for the leader agent's date for the meeting
                            log("\t[waitNewP]:\twait the date of the leader");
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
                                    log("\t[waitNewP]:\tACCEPT the date");
                                } else {
                                    //we have to refuse the proposition
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContent("false");
                                    count_disagree++;
                                    REFUSE = true;
                                    log("\t[waitNewP]:\tREFUSE the date");
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
                            log("\t[waitNewP]:\tMeeting agree. END");
                            step = 4;
                            // END OF DEFINE MEETING ? MAYBE WE STOP HERE ?
                        }//if everybody has responded to the proposal some REFUSE
                        if (count_agree + count_disagree == meetingAgents.length) {
                            //if he is only one to REFUSE: he is the new leader, go to step 1
                            if (count_disagree == 1 && REFUSE == true) {
                                step = 1;
                                leader = true;
                                log("\t[waitNewP]:\tMeeting REFUSE : NEW LEADER");
                                //break;
                            }//else some refuse but not the agent, he waits for proposal step2
                            else {
                                step = 2;
                                log("\t[waitNewP]:\tMeeting REFUSE : wait new meeting");
                            }
                            //if multiple and the agent said REFUSE, he goes to battle step 3
                            if (count_disagree > 1 && REFUSE == true) {
                                step = 3;
                                log("\t[waitNewP]:\tMeeting REFUSE : GO BATTLE");
                            }
                        }
                        break;
                    }
                    // ---------- battleLeader --------------------
                case 3:

                    log("\t[battleLeader]:\tEnter in battleLeader");
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber();
                        randomNumberSend = true;
                        log("\t[battleLeader]:\tSend random number =" + myAleat);
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
                                log("\t[battleLeader]:\tWIN THE BATTLE with" + myAleat);
                                step = 1;
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                log("\t[battleLeader]:\tLoose the battle :" + myAleat);
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
                    log("\t[END]:\tEND");
                    break;

            }
        }

        public boolean done() {
            log(" --> DONE");
            if (step==4)
                return true;
            else
                return false;
        }


        public double sendRandomNumber() {
            double aleat = Math.random();
            ACLMessage sendMess = new ACLMessage(ACLMessage.CFP);
            sendMess.setPerformative(ACLMessage.PROPAGATE);
            sendMess.setContent(String.valueOf((aleat)));
            myAgent.send(sendMess);
            return aleat;
        }


        public void log(String log)
        {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
            // display time and date using toString()
            System.out.println("["+ft.format(date)+"]["+getAID().getLocalName()+"] " +log);
        }

    }


}
