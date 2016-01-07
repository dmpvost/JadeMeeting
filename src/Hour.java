/**
 * Created by vincent on 07/01/16.
 */
public class Hour {

    private double event;

    public Hour()
    {
        double available = Math.random();
        // give 40% to the agent to not be available.
        if(available<0.4)
            event = 0;
        else
            event = Math.random() ;
    }

    public boolean getStatus()
    {
        if ( event == 0 )   // Hour is FULL
            return FALSE;
        else
            return TRUE;    // Hour is FREE for meeting
    }

    public void setStatus(int value)
    {
        event = value ;
    }


}
