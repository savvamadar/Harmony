import java.util.Date;

public class PC
{
        public String pc_id = "";
        public String pc_ip = "";
        public int buttonid=-1;
        
        public static Date dateItem;
        	
        public PC up;
        public PC down;
        public PC left;
        public PC right;
        public long lastHeartBeat;
        public boolean isOnline = false;

        public PC() {}

        public PC(String id, String ip)
        {
            pc_id = id;
            pc_ip = ip;
            buttonid = -1;
            if(dateItem == null) {
            	dateItem = new Date();
            }
        }
        
        public void setHeartBeat() {
        	lastHeartBeat = dateItem.getTime();
        	isOnline = true;
        }

}