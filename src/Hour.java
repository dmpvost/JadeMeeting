/**
 * Created by vincent on 07/01/16.
 */
public class Hour {

    private double  event;
    private boolean look;
    /* this variable is for check if they have look this event or not.
    For don't propose 2 times sames events. */
    // LOOK by default = TRUE, mean CAN BE LOOK.
    private int day;
    /* save the index of the date calendar in hour.*/

    public Hour(int day)
    {
        look = true;
        this.day = day ;
        double available = Math.random();
        // give 40% to the agent to not be available.
        if(available<0.4)
            event = 0;
        else
            event = Math.random() ;
    }


    public double getValue()
    {
        return event;
    }

    public void setValue(int value)
    {
        event = value ;
    }

    // save that this hour have been propose to the others agent.
    public void beLook()
    {
        look=false;
    }

    // return status of Look
    // TRUE : mean was not check
    // FALSE : mean was check
    public boolean LookStatus()
    {
        return look;
    }



}
