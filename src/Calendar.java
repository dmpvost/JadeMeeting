/**
 * Created by vincent on 07/01/16.
 */
public class Calendar {

    private Day[] tabDays;
    private int nbdays;
    private int nbhours;
    private int nbRDVcheck=0;

    public Calendar(int nbdays,int nbhours,double proba) {
        nbRDVcheck=0;
        this.nbdays=nbdays;
        this.nbhours=nbhours;
        tabDays = new Day[nbdays];
        for (int i = 0; i < tabDays.length; i++)
            tabDays[i] = new Day(i,nbhours,proba);
    }

    public double checkFreeHour(int day, int hour) {
        nbRDVcheck++;
        return tabDays[day].getStatusHours(hour);
    }

    public void putMeetingInDate(int day, int hour) {
        nbRDVcheck++;
        tabDays[day].setStatusHours(hour, 0);
    }

    public Hour getBestHour() {
        double best = 0;
        int best_day = 0;
        nbRDVcheck++;

        for (int i = 0; i < tabDays.length; i++) {
            if (best < tabDays[i].getPreferedHour().getValue()) {
                best = tabDays[i].getPreferedHour().getValue();
                best_day = i;
            }
        }
        // save that this preference was check.
        tabDays[best_day].getPreferedHour().setToLook();
        // return Hour object with all information, date, hour and value
        return tabDays[best_day].getPreferedHour();
    }

    public int sizeCal()
    {
        return nbdays*nbhours;
    }

    public int getNbRDVcheck()
    {
        return nbRDVcheck;
    }
}


