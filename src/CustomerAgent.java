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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by user on 07/01/2016.
 */

public class CustomerAgent extends Agent {

    private boolean leader;
    private Calendar cal;
    private boolean ACCEPT = false;
    private boolean done_exec = false;
    private String LogStatus = "";
    //list of found CustomerAgent for meeting
    private AID[] meetingAgents;
    //private int step=0;

    public boolean clavier = false;

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
    private int NUMBER_OF_MEETING_SAVE = 5;
    private int NUMBER_OF_MEETING = NUMBER_OF_MEETING_SAVE;
    private int converseID = 1;

    protected void setup() {

        leader = false;
        System.out.print(" START");
        cal = new Calendar();

        if (this.getLocalName().equals("Arthur"))
            color = ANSI_YELLOW;
        else if (this.getLocalName().equals("Pierre"))
            color = ANSI_CYAN;
        else
            color = ANSI_PURPLE;

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

                        // CLEAN THE TABLE OF AGENTS
                        meetingAgents = new AID[agents.length - 3];
                        int i = 0, y = 0;
                        while (i < (agents.length)) {
                            if (!((agents[i].getName().getLocalName().equals("ams"))
                                    || (agents[i].getName().getLocalName().equals("df"))
                                    || (agents[i].getName().getLocalName().equals("rma")))) {
                                meetingAgents[y] = agents[i].getName();
                                y++;
                            }
                            i++;
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    //ADD BEHAVIOUR HERE
                    done_exec = true;
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


    private class masterBehavior extends Behaviour {
        private int step = 0;
        private int count_disagree = 0;
        private int count_agree = 0;


        //schedule
        private String day;
        private String hour;
        private boolean stepWait = false;

        // battle
        private int bestAleat = -1;
        private int repliesCnt = 0;

        private double myAleat = 0;
        private boolean randomNumberSend = false;

        public void action() {

            switch (step) {


                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    LogStatus = "INIT";
                    logV("INIT", count_agree, count_disagree, ACCEPT);
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    LogStatus = "LEAD";
                    // 1. Become the new leader
                    leader = true;
                    logV("become LEADER", count_agree, count_disagree, ACCEPT);
                    if (clavier == true)
                        pauseProg();
                    // 2. Look for the best date to propose a meeting
                    Hour meeting = cal.getBestHour();
                    logV("best time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour(), count_agree, count_disagree, ACCEPT);
                    day = Integer.toString(meeting.getDay());
                    hour = Integer.toString(meeting.getHour());

                    // 3. Send this date as CFP
                    String proposal_date = meeting.getDay() + "-" + meeting.getHour();
                    sendACLmessToAll(proposal_date, ACLMessage.PROPOSE, converseID + ":meeting_ask","PROPOSE");

                    // 4. switch to waitNewProposal
                    step = 2;
                    count_agree++;
                    ACCEPT = true;
                    //logV(" -> waitNewProposal", count_agree, count_disagree, ACCEPT);
                    break;

                // ---------- waitNewProposal --------------------
                case 2:
                    LogStatus = "WAIT";
                    //1. if not the leader,
                    if (leader == false && stepWait == false) {

                        // a. wait for the leader agent's date for the meeting
                        logV("wait the date of the leader", count_agree, count_disagree, ACCEPT);

                        pauseProg();
                        //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        ACLMessage msg = receive();
                        if (msg != null && (msg.getPerformative() == ACLMessage.PROPOSE) && (msg.getConversationId().equals(converseID + ":meeting_ask"))) {
                            String date = msg.getContent();
                            logRECV("message from [" + msg.getSender().getLocalName() + "]", count_agree, count_disagree, ACCEPT, msg.getSender().getLocalName(), msg.getConversationId());

                            // REPLY TO ALL AGENTS

                            //find if the hour and the day is possible for the agent:
                            String[] parts = date.split("-");
                            day = parts[0];
                            hour = parts[1];
                            count_agree++; // simule le choix du leader

                            double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                            if (possibility != 0) {
                                //We can accept the proposition and return true
                                count_agree++;
                                ACCEPT = true;
                                sendACLmessToAll("true",ACLMessage.ACCEPT_PROPOSAL,converseID+":meeting_answer","ACCEPT_PROPOSAL");
                            } else {
                                //we have to refuse the proposition
                                count_disagree++;
                                ACCEPT = false;
                                sendACLmessToAll("false",ACLMessage.REJECT_PROPOSAL,converseID+":meeting_answer","REJECT_PROPOSAL");
                            }
                            stepWait = true;

                        } else {
                            block();
                        }
                    }

                    ACLMessage msg = myAgent.receive();
                    if ((msg != null)) {
                        logRECV("message from [" + msg.getSender().getLocalName() + "]", count_agree,
                                count_disagree, ACCEPT, msg.getSender().getLocalName(), msg.getConversationId());
                        if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                                && msg.getConversationId().equals(converseID + ":meeting_answer"))
                            count_agree++;
                        if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL
                                && msg.getConversationId().equals(converseID + ":meeting_answer"))
                            count_disagree++;

                        // RECEV ALL MESS
                        if (count_agree + count_disagree == meetingAgents.length) {
                            logV("TAKE DECISION", count_agree, count_disagree, ACCEPT);
                            // CONVERSATION ID must BE INCREMENT HERE
                            converseID++;
                            // CASE 1
                            if (count_agree == meetingAgents.length) {
                                //we fix the schedule
                                logV("1.Meeting AGREE. END", count_agree, count_disagree, ACCEPT);
                                if (NUMBER_OF_MEETING != 0) {
                                    step = 3;
                                    NUMBER_OF_MEETING--;
                                } else {
                                    step = 4;
                                }
                                cal.putMeetingInDate(Integer.parseInt(day), Integer.parseInt(hour));
                            } else if (count_disagree == 1 && ACCEPT == false) {
                                step = 1;
                                leader = true;
                                logV("2.Meeting REFUSE by all others : BECOME NEW LEADER", count_agree, count_disagree, ACCEPT);
                            }//else some refuse but not the agent, he waits for proposal step2
                            else if (count_disagree > 1 && ACCEPT == false) {//
                                step = 3;
                                logV("3.Meeting REFUSE : GO BATTLE", count_agree, count_disagree, ACCEPT);
                            } else {
                                step = 2;
                                // don't participe to the battle, but send message
                                logV("4.Meeting REFUSED by OTHERS : wait new meeting", count_agree, count_disagree, ACCEPT);
                                sendRandomNumber(false);
                            }
                            leader = false;
                            ACCEPT = false;
                            count_agree = 0;
                            count_disagree = 0;
                            stepWait = false;
                            if (clavier == true)
                                pauseProg();

                        }
                    } else {
                        block();
                    }
                    //}
                    break;

                // ---------- battleLeader --------------------
                case 3:
                    LogStatus = "BATL";

                    //logV("Enter in battleLeader randomNumberSend=" + randomNumberSend, count_agree, count_disagree, ACCEPT);
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = sendRandomNumber(true);
                        randomNumberSend = true;
                        repliesCnt++;

                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    ACLMessage reply = myAgent.receive();
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM || reply.getConversationId().equals(converseID + ":random")) {
                            //proposal received
                            logRECV(" message from [" + reply.getSender().getLocalName() + "] =>" + reply.getContent().toString(),
                                    count_agree, count_disagree, ACCEPT, reply.getSender().getLocalName(), reply.getConversationId());
                            int getAleat = Integer.parseInt(reply.getContent());
                            if (getAleat > bestAleat) {
                                bestAleat = getAleat;
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= meetingAgents.length) {
                            //all proposals have been received
                            if (myAleat > bestAleat) {
                                logV("WIN THE BATTLE with " + myAleat, count_agree, count_disagree, ACCEPT);
                                step = 1;
                                if (clavier == true)
                                    pauseProg();
                                // WIN THE BATTLE -> become leader
                            } else {
                                step = 2;
                                logV("LOOSE the battle :" + myAleat + " - best=" + bestAleat, count_agree, count_disagree, ACCEPT);
                                // loose the game go to waitNewProposal
                                if (clavier == true)
                                    pauseProg();
                            }

                            bestAleat = 0;
                            myAleat = 0;
                            repliesCnt = 0;
                            randomNumberSend = false;
                        }

                    } else {
                        block();
                    }
                    break;
                case 4:
                    LogStatus = "E";
                    logV("END", count_agree, count_disagree, ACCEPT);
                    Scanner keyboard = new Scanner(System.in);
                    keyboard.nextLine();
                    break;

            }
        }

        public boolean done() {
            if (step == 4 && NUMBER_OF_MEETING == 0) {
                return true;
            } else {
                return false;
            }
        }

        private void addAllReceiverToMess(ACLMessage msg) {
            for (int i = 0; i < meetingAgents.length; i++) {
                if (!(meetingAgents[i].getLocalName().equals(myAgent.getLocalName()))) {
                    msg.addReceiver(meetingAgents[i]);
                }
            }
        }

        public double sendRandomNumber(boolean play) {

            int aleat;
            if (play == true) {
                aleat = (int) (Math.random() * 10000);
            } else {
                aleat = 0;
            }

            sendACLmessToAll(String.valueOf(aleat), ACLMessage.INFORM, converseID + ":random","INFORM");

            return aleat;
        }

        public void log(String log) {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
            System.out.println("[" + ft.format(date) + "][" + getAID().getLocalName() + "]\t" + log);
        }

        public void logV(String log, int a, int b, boolean c) {
            logVV(log, a, b, c, "");
        }

        public void logSEND(String log, int a, int b, boolean c, String id,String type) {
            logVV(log, a, b, c, ANSI_GREEN + "[<-][SEND|"+id+"]{" + type + "}");
        }

        public void logRECV(String log, int a, int b, boolean c, String owner, String id) {
            logVV(log, a, b, c, getColor(owner) + "[->][RECV]{" + id + "}");
        }

        public void logVV(String log, int a, int b, boolean c, String state) {

            try {
                TimeUnit.MILLISECONDS.sleep(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");

            String cc = "";
            if (LogStatus.equals("BATL"))
                cc = ANSI_RED;
            else if (LogStatus.equals("LEAD"))
                cc = ANSI_GREEN;

            String ff = "";
            if (c == true)
                ff = ANSI_GREEN;
            else
                ff = ANSI_RED;

            System.out.println("[" + ft.format(date) + "]"
                    + "[" + NUMBER_OF_MEETING + "/" + NUMBER_OF_MEETING_SAVE + "]"
                    + ANSI_GREEN + a + ANSI_RESET + "|"
                    + ANSI_RED + b + ANSI_RESET + "|"
                    + ff + c + ANSI_RESET + "]["
                    + cc + LogStatus + ANSI_RESET + "]["
                    + color + getAID().getLocalName() + ANSI_RESET
                    + "[" + converseID
                    + "]\t" + state + log + ANSI_RESET);

        }


        private void sendACLmessToAll(String content, int performative, String convID,String type) {
            ACLMessage message = new ACLMessage(ACLMessage.CFP);
            addAllReceiverToMess(message);
            message.setContent(content);
            message.setPerformative(performative);
            message.setConversationId(convID);
            logSEND(" content=" + content, count_agree, count_disagree, ACCEPT, message.getConversationId(),type);
            send(message);
            if (clavier == true)
                pauseProg();
        }

    }


    public void pauseProg() {
        if (clavier == true) {
            Scanner keyboard = new Scanner(System.in);
            keyboard.nextLine();
        }
    }

    public String getColor(String name) {
        String ownercolor = "";
        if (name.equals("Arthur"))
            ownercolor = ANSI_YELLOW;
        else if (name.equals("Pierre"))
            ownercolor = ANSI_CYAN;
        else
            ownercolor = ANSI_PURPLE;

        return ownercolor;
    }


}
