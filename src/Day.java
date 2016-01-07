/**
 * Created by vincent on 07/01/16.
 */
public class Day {

    private Hour[] tabHours ;

    public Day()
    {
        tabHours = new Hour[3];
        for (int i = 0 ; i < tabHours.length ; i++)
            tabHours[i] = new Hour();
    }

    public boolean getStatusHours(int index)
    {
        return tabHours[index].getStatus();
    }

    public void setStatusHours(int index,int value)
    {
        tabHours[index].setStatus(value);
    }

}
