/**
 * Created by vincent on 07/01/16.
 */
public class Hour {

    private double event;
    private boolean look = false;
    /* this variable is to check if they already looked
    to not propose 2 times sames events. */
    // LOOK by default = FALSE, mean CAN BE LOOK.
    private int day;
    private int hour;
    /* save the index of the date calendar in hour.*/

    public Hour(int day, int hour) {
        look = false;
        this.day = day;
        this.hour = hour;
        double available = Math.random();
        // give 40% to the agent to not be available.
        if (available < 0.4)
            event = 0;
        else
            event = Math.random();
    }


    public double getValue() {
        return event;
    }

    public void setValue(int value) {
        event = value;
    }

    // save that this hour have been propose to the others agent.
    public void setToLook() {
        look = true;
    }

    // return status of Look
    public boolean isLook() {
        return look;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

}
