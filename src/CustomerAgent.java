import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by user on 07/01/2016.
 */

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
                        //log(": the following AGENT have been found");

                        // CLEAN THE TABLE OF AGENTS
                        meetingAgents = new AID[agents.length - 3];
                        int i = 0, y = 0;
                        while (i < (agents.length)) {
                            if (!((agents[i].getName().getLocalName().equals("ams")) || (agents[i].getName().getLocalName().equals("df")) || (agents[i].getName().getLocalName().equals("rma")))) {
                                meetingAgents[y] = agents[i].getName();
                                //log(meetingAgents[y].getLocalName());
                                y++;
                            }
                            i++;
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    //ADD BEHAVIOUR HERE
                    done_exec = true;
                    //log("Add behavior");
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
        private int NUMBER_OF_MEETING=3;

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
                    leader=true;
                    log("[Leader]:become LEADER");
                    pauseProg();
                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    log("[Leader]:best time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour());
                    day = Integer.toString(meeting.getDay());
                    hour = Integer.toString(meeting.getHour());
                    pauseProg();
                    // 3. Send this date as CFP
                    //call for proposal (CFP) to found agents
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < meetingAgents.length; i++) {
                        if (!(meetingAgents[i].getLocalName().equals(myAgent.getLocalName()))) {
                            cfp.addReceiver(meetingAgents[i]);
                        }
                    }

                    String proposal_date = meeting.getDay() + "-" + meeting.getHour();
                    cfp.setContent(proposal_date);
                    cfp.setPerformative(ACLMessage.PROPOSE); //???? we need to put this or not ?
                    cfp.setConversationId("meeting date");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value

                    myAgent.send(cfp);
                    //messTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(proposal_date),
                      //      MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    log("[Leader]: send request MEETING");
                    pauseProg();
                    // 4. switch to waitNewProposal
                    step = 2;
                    count_agree++;
                    REFUSE=false;
                    log("[Leader]: -> waitNewProposal");
                    //pauseProg();
                    //setLeader(false);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    //if (stepWait == false)
                    //{
                        //1. if not the leader,
                        if (leader == false && stepWait==false)
                        {


                            // a. wait for the leader agent's date for the meeting
                            log("[waitNewP]:wait the date of the leader");
                            pauseProg();
                            //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                            ACLMessage msg = receive();
                            if (msg != null && msg.getPerformative()==ACLMessage.PROPOSE)
                            {
                                String date = msg.getContent();
                                ACLMessage reply = msg.createReply();
                                //find if the hour and the day is possible for the agent:
                                String[] parts = date.split("-");
                                day = parts[0];
                                hour = parts[1];
                                count_agree++; // simule le choix du leader

                                double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                                if (possibility != 0)
                                {
                                    //We can accept the proposition and return true
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContent("true");
                                    count_agree++;
                                    REFUSE = false;
                                    log("[waitNewP]:ACCEPT the date");
                                    pauseProg();
                                }
                                else
                                {
                                    //we have to refuse the proposition
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContent("false");
                                    count_disagree++;
                                    REFUSE = true;
                                    log("[waitNewP]:REFUSE the date");
                                    pauseProg();
                                }
                                stepWait = true;
                                myAgent.send(reply);

                            } else
                            {
                                block();
                            }
                        }
                        //else
                        //{
                         //   stepWait=true;
                        //}
                    //}
                    //else if (stepWait == true)
                    //{
                        //for everybody: receive the responses
                        //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        //ACLMessage msg = myAgent.receive(mt);
                        log("count_agree="+count_agree+" count_disagree="+count_disagree);
                        ACLMessage msg = receive();
                        if (msg != null || (count_agree + count_disagree == meetingAgents.length))
                        {

                            if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                                count_agree++;
                            if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL)
                                count_disagree++;
                            log("count_agree="+count_agree+" count_disagree="+count_disagree+ "REFUSE="+REFUSE);
                            // MEETING FIXED
                            if (count_agree == meetingAgents.length)
                            {
                                //we fix the schedule
                                log("[waitNewP]:Meeting AGREE. END");
                                if(NUMBER_OF_MEETING!=0)
                                {
                                    step = 3;
                                    NUMBER_OF_MEETING--;
                                }
                                else
                                {
                                    step=4;
                                }
                                //cal.putMeetingInDate(Integer.parseInt(day), Integer.parseInt(hour));
                                //go to step 3 battle to know the next leader
                                pauseProg();

                                // END OF DEFINE MEETING ? MAYBE WE STOP HERE ?
                            }
                            // RECEV ALL MESS BUT DONT AGREE
                            else if (count_agree + count_disagree == meetingAgents.length)
                            {

                                log("TAKE DECISION");
                                //if he is only one to REFUSE: he is the new leader, go to step 1
                                if (count_disagree == 1 && REFUSE == true)
                                {
                                    step = 1;
                                    leader = true;
                                    log("[waitNewP]:Meeting REFUSE : BECOME NEW LEADER");
                                    pauseProg();
                                }//else some refuse but not the agent, he waits for proposal step2
                                else if (count_disagree > 1 && REFUSE == true)
                                {//
                                    step = 3;
                                    log("[waitNewP]:Meeting REFUSE : GO BATTLE");
                                    pauseProg();
                                }
                                else
                                {
                                    step = 2;
                                    log("[waitNewP]:Meeting REFUSED by OTHERS : wait new meeting");
                                    pauseProg();
                                }
                                leader = false;
                                REFUSE=false;
                                count_agree=0;
                                count_disagree=0;

                            }
                        }
                        else
                        {
                            block();
                        }
                    //}
                    break;

                    // ---------- battleLeader --------------------
                case 3:


                    log("[battleLeader]:Enter in battleLeader randomNumberSend=" + randomNumberSend);
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber();
                        randomNumberSend = true;
                        repliesCnt++;
                        log("[battleLeader]:Send random number =" + myAleat);
                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    //ACLMessage reply = myAgent.receive(mt);
                    ACLMessage reply = receive();
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            //proposal received
                            int getAleat = Integer.parseInt(reply.getContent());
                            //log(" compare" + getAleat + "and " + bestAleat);
                            if ( getAleat > bestAleat) {
                                //the best proposal as for now
                                bestAleat = getAleat;
                                bestAgent = reply.getSender();
                                log("bestAleat found = "+bestAleat);
                            }
                        }
                        repliesCnt++;
                        //log("meetingAgents.length="+meetingAgents.length);
                        if (repliesCnt >= meetingAgents.length) {
                            //all proposals have been received
                            if (myAleat > bestAleat) {
                                log("[battleLeader]:WIN THE BATTLE with" + myAleat);
                                step = 1;
                                pauseProg();
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                log("[battleLeader]:Loose the battle :" + myAleat);
                                // loose the game go to waitNewProposal
                                pauseProg();
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
            //log(" --> DONE step=" + step + " randomNumberSend=" + randomNumberSend);
            if (step == 4 && NUMBER_OF_MEETING==0) {
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

            return aleat;
        }


    }

    public void log(String log) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
        // display time and date using toString()

        System.out.println("[" + ft.format(date) + "][" + getAID().getLocalName() + "]\t" + log);
    }

    public  void pauseProg(){
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
    }
}
