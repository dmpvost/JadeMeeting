
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
    private Hashtable catalogue;
    private BookSellerGui myGui;

    private int shippingCost;

    protected void setup() {
        catalogue = new Hashtable();
        myGui = new BookSellerGui(this);
        myGui.display();

        Object[] args = getArguments();
        if (args != null && args.length > 0) shippingCost = Integer.parseInt(args[0].toString());

        //book selling service registration at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());

        addBehaviour(new PurchaseOrdersServer());
    }

    protected void takeDown() {
        //book selling service deregistration at DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        myGui.dispose();
        System.out.println("Seller agent " + getAID().getName() + " terminated.");
    }

    //invoked from GUI, when a new book is added to the catalogue
    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(title, new Integer(price));
                int total = price + shippingCost;
                System.out.println(getAID().getLocalName() + ": " + title + " put into the catalogue. Price = " + price + " + shipping"
                        + shippingCost + "Total =" + total);
            }
        });
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            //proposals only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
                    //title found in the catalogue, respond with its price as a proposal
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue() + shippingCost));
                } else {
                    //title not found in the catalogue
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }


    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            //purchase order as proposal acceptance only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getAID().getLocalName() + ": " + title + " sold to " + msg.getSender().getLocalName());
                } else {
                    //title not found in the catalogue, sold to another agent in the meantime (after proposal submission)
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

}
