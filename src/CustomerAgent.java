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

        // --------------- INIT -------------------------------
        // 1: Numbers of days
        // 2: Number of hours in one days
        // 3: how many calendar is full 0.3 mean 30% is full
        cal = new Calendar(5, 8, 0.3);
        // Number of meeting that AGENT need to find
        NUMBER_OF_MEETING_SAVE = 5;

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

                    color = getColor(myAgent.getLocalName());

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
        private int nbMessACLrecv = 0;

        //schedule
        private String day;
        private String hour;
        private boolean stepWait = false;

        // battle
        private int bestAleat = -1;
        private int repliesCnt = 0;

        private int myAleat = 0;
        private boolean randomNumberSend = false;

        public void action() {

            switch (step) {


                // INIT -> WHO IS GOING TO BE Leader
                case 0:
                    LogStatus = "INIT";
                    logV("INIT");
                    count_disagree = meetingAgents.length;
                    step = 3; // go to find the leader.
                    break;

                // ---------- Leader --------------------
                case 1:
                    if (cal.sizeCal() > cal.getNbRDVcheck()) {
                        LogStatus = "LEAD";
                        leader = true;
                        logV(ANSI_RED+"[DateCheck:"+cal.getNbRDVcheck()+"/"+cal.sizeCal()+"] "+myAgent.getLocalName()+" become LEADER"+ANSI_RESET);
                        Hour meeting = cal.getBestHour();
                        if(meeting.getHour()!=-1) {
                            logV("best time for meeting is day:" + meeting.getDay() + " at " + meeting.getHour());
                            day = Integer.toString(meeting.getDay());
                            hour = Integer.toString(meeting.getHour());

                            // 3. Send this date as CFP
                            String proposal_date = meeting.getDay() + "-" + meeting.getHour();
                            sendACLmessToAll(proposal_date, ACLMessage.PROPOSE, converseID + ":meeting_ask", "PROPOSE");

                            // 4. switch to waitNewProposal
                            step = 2;
                            count_agree++;
                            ACCEPT = true;
                        }
                        else
                        {
                            logV("ERRROR");
                            step=5;
                        }
                    } else {
                        step = 5;
                        // NO PLACE IN CALENDAR
                    }
                    break;

                // ---------- waitNewProposal --------------------
                case 2:

                    LogStatus = "WAIT";
                    //1. if not the leader,

                    if (leader == false && stepWait == false) {
                        // a. wait for the leader agent's date for the meeting
                        logV("wait the date of the leader");

                        ACLMessage content = recvACLmess(ACLMessage.PROPOSE, converseID + ":meeting_ask", "PROPOSE");
                        if (content != null) {

                            String[] parts = content.getContent().toString().split("-");
                            day = parts[0];
                            hour = parts[1];
                            count_agree++; // simule le choix du leader

                            // REPLY TO ALL AGENTS
                            double possibility = cal.checkFreeHour(Integer.parseInt(day), Integer.parseInt(hour));
                            if (possibility != 0) {
                                //We can accept the proposition and return true
                                count_agree++;
                                ACCEPT = true;
                                logV("ACCEPT_PROPOSAL MEETING " + content.getContent().toString());
                                sendACLmessToAll("true", ACLMessage.ACCEPT_PROPOSAL, converseID + ":meeting_answer", "ACCEPT_PROPOSAL");
                            } else {
                                //we have to refuse the proposition
                                count_disagree++;
                                ACCEPT = false;
                                logV("REJECT_PROPOSAL MEETING " + content.getContent().toString());
                                sendACLmessToAll("false", ACLMessage.REJECT_PROPOSAL, converseID + ":meeting_answer", "REJECT_PROPOSAL");
                            }
                            stepWait = true;
                        }

                    }


                    // RECEPTION ALL MESSAGE
                    recvACLmessPROPOSAL(converseID + ":meeting_answer");

                    if (count_agree + count_disagree == meetingAgents.length) {
                        logV("TAKE DECISION");
                        // CONVERSATION ID must BE INCREMENT HERE
                        converseID++;
                        // CASE 1
                        if (count_agree == meetingAgents.length) {
                            //we fix the schedule
                            logV(ANSI_RED+"[DateCheck:"+cal.getNbRDVcheck()+"/"+cal.sizeCal()+"] 1.Meeting AGREE. END"+ANSI_RESET);
                            if (NUMBER_OF_MEETING != 0) {
                                step = 3;
                                NUMBER_OF_MEETING--;
                            } else {
                                step = 4;
                            }
                            count_disagree = meetingAgents.length;// NO RESET TO 0
                            cal.putMeetingInDate(Integer.parseInt(day), Integer.parseInt(hour));
                        } else if (count_disagree == 1 && ACCEPT == false) {
                            step = 1;
                            leader = true;
                            count_disagree = 0;
                            logV("2.Meeting REFUSE by all others : BECOME NEW LEADER");
                        }//else some refuse but not the agent, he waits for proposal step2
                        else if (count_disagree > 1 && ACCEPT == false) {//
                            step = 3;
                            //count_disagree = 0; // NOT RESET BECAUSE WE NEED IT
                            logV("3.Meeting REFUSE : GO BATTLE");
                        } else {
                            step = 2;
                            count_disagree = 0;
                            // don't participe to the battle, but send message
                            logV("4.Meeting REFUSED by OTHERS : wait new meeting");
                            //sendACLmessToAll(Integer.toString(0), ACLMessage.INFORM, converseID + ":random", "INFORM");
                        }
                        leader = false;
                        ACCEPT = false;
                        stepWait = false;
                        count_agree = 0;
                    }

                    break;

                // ---------- battleLeader --------------------
                case 3:
                    LogStatus = "BATL";

                    //logV("Enter in battleLeader randomNumberSend=" + randomNumberSend, count_agree, count_disagree, ACCEPT);
                    // SendRandom number to other agent
                    if (randomNumberSend == false) {
                        myAleat = randomNumber();
                        sendACLmessToAll(Integer.toString(myAleat), ACLMessage.INFORM, converseID + ":random", "INFORM");
                        randomNumberSend = true;
                        repliesCnt++;
                    }

                    // Collect all proposals (cycle zone)
                    //collect proposals
                    ACLMessage content = recvACLmess(ACLMessage.INFORM, converseID + ":random", "INFORM");
                    if (content != null) {
                        int getAleat = Integer.parseInt(content.getContent());
                        if (getAleat > bestAleat) {
                            bestAleat = getAleat;
                        }

                        repliesCnt++;
                        if (repliesCnt >= count_disagree) {
                            //all proposals have been received
                            if (myAleat > bestAleat) {
                                logV("WIN THE BATTLE with " + myAleat);
                                step = 1;
                            } else {
                                step = 2;
                                logV("LOOSE the battle :" + myAleat + " - best=" + bestAleat);
                            }

                            bestAleat = 0;
                            myAleat = 0;
                            repliesCnt = 0;
                            randomNumberSend = false;
                            count_disagree = 0;
                        }
                    }
                    break;
                case 4:
                    LogStatus = "E";
                    logV(ANSI_RED+"[DateCheck:"+cal.getNbRDVcheck()+"/"+cal.sizeCal()+"] END ALL OK"+ANSI_RESET);
                    Scanner keyboard = new Scanner(System.in);
                    keyboard.nextLine();
                    break;
                case 5:
                    LogStatus = "E";
                    logV(ANSI_RED+"[DateCheck:"+cal.getNbRDVcheck()+"/"+cal.sizeCal()+"] END ALL POSSIBILITY TRY -> NO DATE FOUND"+ANSI_RESET);
                    Scanner keyboard2 = new Scanner(System.in);
                    keyboard2.nextLine();
                    break;

            }
        }

        public boolean done() {
            if (step == 4 && NUMBER_OF_MEETING == 0) {
                logV(ANSI_RED+"[DateCheck:"+cal.getNbRDVcheck()+"/"+cal.sizeCal()+"] END ALL OK"+ANSI_RESET);
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

        public int randomNumber() {
            int aleat = (int) (Math.random() * 10000);
            return aleat;
        }

        public void log(String log) {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss.SS");
            System.out.println("[" + ft.format(date) + "][" + getAID().getLocalName() + "]\t" + log);
        }

        public void logV(String log) {
            logVV(log, "");
        }

        public void logSEND(String log, String id, String type) {
            logVV(log, ANSI_GREEN + "[<-][SEND|" + id + "]{" + type + "}");
        }

        public void logRECV(String log, String owner, String id) {
            logVV(log, getColor(owner) + "[->][RECV]{" + id + "}");
        }


        public void logVV(String log, String state) {

            try {
                TimeUnit.MILLISECONDS.sleep(40);
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
            if (ACCEPT == true)
                ff = ANSI_GREEN;
            else
                ff = ANSI_RED;

            System.out.println("[" + ft.format(date) + "]"
                    + "[" + NUMBER_OF_MEETING + "/" + NUMBER_OF_MEETING_SAVE + "]"
                    + ANSI_GREEN + count_agree + ANSI_RESET + "|"
                    + ANSI_RED + count_disagree + ANSI_RESET + "|"
                    + ff + ACCEPT + ANSI_RESET + "]["
                    + cc + LogStatus + ANSI_RESET + "]["
                    + color + getAID().getLocalName() + ANSI_RESET
                    + "[" + converseID
                    + "]\t" + state + log + ANSI_RESET);

        }


        private void sendACLmessToAll(String content, int performative, String convID, String performative_string) {
            ACLMessage message = new ACLMessage(ACLMessage.CFP);
            addAllReceiverToMess(message);
            message.setContent(content);
            message.setPerformative(performative);
            message.setConversationId(convID);
            logSEND(" content=" + content, message.getConversationId(), performative_string);
            send(message);
            if (clavier == true)
                pauseProg();
        }

        private ACLMessage recvACLmess(int ACLperformative, String converseID, String performative_string) {
            ACLMessage recv = myAgent.receive();
            String content = null;
            if (recv != null) {
                if (recv.getPerformative() == ACLperformative && recv.getConversationId().equals(converseID)) {
                    logRECV(" message from [" + recv.getSender().getLocalName() + "] =>" + recv.getContent().toString()
                            , recv.getSender().getLocalName(), recv.getConversationId());
                    //content = recv.getContent();
                    return recv;
                }
            } else {
                //logV("WAIT {"+converseID+"}"+"{"+performative_string+"}");
                block();
            }
            return null;
        }

        private void recvACLmessPROPOSAL(String converseID) {
            ACLMessage recv = myAgent.receive();
            if (recv != null && recv.getConversationId().equals(converseID)) {
                if (recv.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                    count_agree++;
                if (recv.getPerformative() == ACLMessage.REJECT_PROPOSAL)
                    count_disagree++;

                logRECV(" message from [" + recv.getSender().getLocalName() + "] =>" + recv.getContent().toString()
                        , recv.getSender().getLocalName(), recv.getConversationId());
            } else {
                block();
            }
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
        else if (name.equals("Matt"))
            ownercolor = ANSI_PURPLE;
        else if (name.equals("Vincent"))
            ownercolor = ANSI_GREEN;
        else if (name.equals("Mat"))
            ownercolor = ANSI_BLUE;
        else
            ownercolor = ANSI_RED;

        return ownercolor;
    }


}
