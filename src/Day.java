/**
 * Created by vincent on 07/01/16.
 */
public class Day {

    private Hour[] tabHours ;

    public Day(int day)
    {
        tabHours = new Hour[3];
        for (int i = 0 ; i < tabHours.length ; i++) {
            tabHours[i] = new Hour(day);
        }
    }

    public double getStatusHours(int index)
    {
        return tabHours[index].getValue();
    }

    public void setStatusHours(int index,int value)
    {
        tabHours[index].setValue(value);
    }

    public Hour getPreferenceHour()
    {
        double best=0;
        int hour=0;

        for(int i=0 ; i < tabHours.length ; i++)
        {
            if(best<tabHours[i].getValue())
            {
                best = tabHours[i].getValue();
                hour = i ;
            }
        }
        return tabHours[hour];
    }


}
