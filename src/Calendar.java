/**
 * Created by vincent on 07/01/16.
 */
public class Calendar {

    private Day[] tabDays;

    public Calendar()
    {
        tabDays = new Day[5];
        for( int i = 0 ; i < tabDays.length ; i++ )
            tabDays[i] = new Day(i);
    }

    public double checkFreeHour(int day, int hour)
    {
        return tabDays[day].getStatusHours(hour);
    }

    public void putMeetingInDate(int day, int hour)
    {
        tabDays[day].setStatusHours(hour,0);
    }

    public Hour getHigherPreference()
    {
        double best=0;
        int best_day = 0;

        for(int i=0 ; i < tabDays.length ; i++)
        {
            if(best<tabDays[i].getPreferenceHour().getValue())
            {
                best = tabDays[i].getPreferenceHour().getValue();
                best_day = i ;
            }
        }
        tabDays[best_day].getPreferenceHour().beLook();
        return tabDays[best_day].getPreferenceHour();
        // return Hour object with all information, date, hour and value
    }

}


