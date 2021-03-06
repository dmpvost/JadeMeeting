/**
 * Created by vincent on 07/01/16.
 */
public class Calendar {

    private Day[] tabDays;

    public Calendar() {
        tabDays = new Day[5];
        for (int i = 0; i < tabDays.length; i++)
            tabDays[i] = new Day(i);
    }

    public double checkFreeHour(int day, int hour) {
        return tabDays[day].getStatusHours(hour);
    }

    public void putMeetingInDate(int day, int hour) {
        tabDays[day].setStatusHours(hour, 0);
    }

    public Hour getBestHour() {
        double best = 0;
        int best_day = 0;

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

}


