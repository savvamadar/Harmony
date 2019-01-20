public class PC
{
        public String pc_id = "";
        public String pc_ip = "";
        public int buttonid=-1;
        	
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
        }
        
        public void setHeartBeat() {
        	lastHeartBeat = System.currentTimeMillis();
        	isOnline = true;
        }

}