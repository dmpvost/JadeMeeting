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
    private boolean ACCEPT = false;
    private boolean done_exec = false;
    private String LogStatus="";
    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;
    //private int step=0;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String color;

    protected void setup() {

        leader = false;
        log(" START");
        cal = new Calendar();



        if(this.getLocalName().equals("Arthur"))
        {
            color=ANSI_YELLOW;
        }
        else
        {
            color=ANSI_CYAN;
        }

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
                    LogStatus="INIT";
                    logV("INIT",count_agree,count_disagree,ACCEPT);
                    // COUNT Number of agent
                    // numberOfAgent=X;
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    LogStatus="LEAD";
                    // 1. Become the new leader
                    setLeader(true);
                    leader=true;
                    logV("become LEADER",count_agree,count_disagree,ACCEPT);
                    pauseProg();
                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    logV("best time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour(),count_agree,count_disagree,ACCEPT);
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
                    logSEND("request MEETING", count_agree, count_disagree, ACCEPT);
                    pauseProg();
                    // 4. switch to waitNewProposal
                    step = 2;
                    count_agree++;
                    ACCEPT=true;
                    logV(" -> waitNewProposal",count_agree,count_disagree,ACCEPT);
                    //pauseProg();
                    //setLeader(false);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    LogStatus="WAIT";
                    //if (stepWait == false)
                    //{
                    //logV("COUNT1",count_agree,count_disagree,ACCEPT);
                        //1. if not the leader,
                        if (leader == false && stepWait==false)
                        {


                            // a. wait for the leader agent's date for the meeting
                            logV("wait the date of the leader",count_agree,count_disagree,ACCEPT);
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

                                logRECV("message from [" + msg.getSender().getLocalName() + "]", count_agree, count_disagree, ACCEPT);

                                double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                                if (possibility != 0)
                                {
                                    //We can accept the proposition and return true
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContent("true");
                                    count_agree++;
                                    ACCEPT = true;
                                    logSEND("ACCEPT the date", count_agree, count_disagree, ACCEPT);
                                    pauseProg();
                                }
                                else
                                {
                                    //we have to refuse the proposition
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    reply.setContent("false");
                                    count_disagree++;
                                    ACCEPT = false;
                                    logSEND("REFUSE the date", count_agree, count_disagree, ACCEPT);
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
                    //logV("COUNT2",count_agree,count_disagree,ACCEPT);
                        ACLMessage msg = receive();
                        if ( ( msg != null || count_agree + count_disagree == meetingAgents.length))
                        {
                            if(msg !=null) {
                                logRECV("message from [" + msg.getSender().getLocalName() + "]", count_agree, count_disagree, ACCEPT);
                                if ( msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                                    count_agree++;
                                if ( msg.getPerformative() == ACLMessage.REJECT_PROPOSAL)
                                    count_disagree++;
                            }
                            //logV("COUNT3",count_agree,count_disagree,ACCEPT);
                            // MEETING FIXED
                            if (count_agree == meetingAgents.length)
                            {
                                //we fix the schedule
                                logV("Meeting AGREE. END", count_agree, count_disagree, ACCEPT);
                                if(NUMBER_OF_MEETING!=0)
                                {
                                    step = 3;
                                    NUMBER_OF_MEETING--;
                                }
                                else
                                {
                                    step=4;
                                }
                                cal.putMeetingInDate(Integer.parseInt(day), Integer.parseInt(hour));
                                //go to step 3 battle to know the next leader
                                pauseProg();
                                leader = false;
                                ACCEPT=false;
                                stepWait=false;
                                count_agree=0;
                                count_disagree=0;

                                // END OF DEFINE MEETING ? MAYBE WE STOP HERE ?
                            }
                            // RECEV ALL MESS BUT DONT AGREE
                            else if (count_agree + count_disagree == meetingAgents.length)
                            {

                                logV("TAKE DECISION",count_agree,count_disagree,ACCEPT);
                                //if he is only one to REFUSE: he is the new leader, go to step 1
                                if (count_disagree == 1 && ACCEPT == false)
                                {
                                    step = 1;
                                    leader = true;
                                    logV("Meeting REFUSE : BECOME NEW LEADER",count_agree,count_disagree,ACCEPT);
                                    pauseProg();
                                }//else some refuse but not the agent, he waits for proposal step2
                                else if (count_disagree > 1 && ACCEPT == false)
                                {//
                                    step = 3;
                                    logV("Meeting REFUSE : GO BATTLE",count_agree,count_disagree,ACCEPT);
                                    pauseProg();
                                }
                                else
                                {
                                    step = 2;
                                    logV("Meeting REFUSED by OTHERS : wait new meeting",count_agree,count_disagree,ACCEPT);
                                    pauseProg();
                                }
                                leader = false;
                                ACCEPT=false;
                                count_agree=0;
                                count_disagree=0;
                                stepWait=false;

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
                    LogStatus="BATL";

                    logV("Enter in battleLeader randomNumberSend=" + randomNumberSend,count_agree,count_disagree,ACCEPT);
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber();
                        randomNumberSend = true;
                        repliesCnt++;
                        logSEND("random number =" + myAleat, count_agree, count_disagree, ACCEPT);
                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    //ACLMessage reply = myAgent.receive(mt);
                    ACLMessage reply = receive();
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            //proposal received
                            logRECV(" message from [" + reply.getSender().getLocalName() + "]", count_agree, count_disagree, ACCEPT);
                            int getAleat = Integer.parseInt(reply.getContent());
                            //log(" compare" + getAleat + "and " + bestAleat);
                            if ( getAleat > bestAleat) {
                                //the best proposal as for now
                                bestAleat = getAleat;
                                bestAgent = reply.getSender();
                                logV("bestAleat found = " + bestAleat,count_agree,count_disagree,ACCEPT);
                            }
                        }
                        repliesCnt++;
                        //log("meetingAgents.length="+meetingAgents.length);
                        if (repliesCnt >= meetingAgents.length) {
                            //all proposals have been received
                            if (myAleat > bestAleat) {
                                logV("WIN THE BATTLE with" + myAleat,count_agree,count_disagree,ACCEPT);
                                step = 1;
                                pauseProg();
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                logV("Loose the battle :" + myAleat,count_agree,count_disagree,ACCEPT);
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
                    LogStatus="E";
                    logV("END",count_agree,count_disagree,ACCEPT);
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

    public void logV(String log,int a,int b,boolean c) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
        // display time and date using toString()

        System.out.println("[" + ft.format(date)
                +"]["+a+"|"+b+"|"+c+"]["+LogStatus+"]["
                +color+ getAID().getLocalName() +ANSI_RESET
                + "]\t" + log);
    }

    public void logSEND(String log,int a,int b,boolean c) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
        // display time and date using toString()

        System.out.println("[" + ft.format(date) + "]["+a+"|"+b+"|"+c+"]["+LogStatus+"]["
                +color+ getAID().getLocalName() +ANSI_RESET
                + "]\t" + ANSI_GREEN+"[<-][SEND]"+log+ANSI_RESET);
    }

    public void logRECV(String log,int a,int b,boolean c) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
        // display time and date using toString()

        System.out.println("[" + ft.format(date) +"]["+a+"|"+b+"|"+c+"]["+LogStatus+"]["
                +color+ getAID().getLocalName() +ANSI_RESET
                + "]\t" + ANSI_PURPLE+"[->][RECV]"+log+ANSI_RESET);
    }

    public  void pauseProg(){
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
    }
}
