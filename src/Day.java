/**
 * Created by vincent on 07/01/16.
 */
public class Day {

    private Hour[] tabHours;

    public Day(int day) {
        tabHours = new Hour[3];
        for (int i = 0; i < tabHours.length; i++) {
            tabHours[i] = new Hour(day);
        }
    }

    public double getStatusHours(int index) {
        return tabHours[index].getValue();
    }

    public void setStatusHours(int index, int value) {
        tabHours[index].setValue(value);
    }

    public Hour getPreferedHour() {
        double best = 0;
        int hour = 0;
        boolean find = false;

        //we search the best hour in the day
        for (int i = 0; i < tabHours.length; i++) {
            if (tabHours[i].isLook() == false) {
                if (best < tabHours[i].getValue()) {

                    find = true;
                    best = tabHours[i].getValue();
                    hour = i;
                }
            }
        }

        if (find = false) {
            // this case, is a case of error.
            // We need to think about it after...
            Hour not_found = new Hour(0);
            return not_found;
        } else
            return tabHours[hour];
    }


}
