package fujian;

import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.setting.Setting;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.*;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fujian.SFTPUtil.load_Setting;

public class SftpMonitor {

    private static Setting setting;

    private static final Logger log = LoggerFactory.getLogger(SyncFiles.class);

   static {

       // 获取当前类的保护域
       ProtectionDomain domain = test.class.getProtectionDomain();
       // 获取代码源
       CodeSource codeSource = domain.getCodeSource();
       // 获取代码源的URL
       URL location = codeSource.getLocation();
       try {
           // 获取文件路径
           File jarFile = new File(location.getPath());
           // 获取jar包所在的目录
           // 如果是文件,这个文件指定的是jar所在的路径(注意如果是作为依赖包，这个路径是jvm启动加载的jar文件名)

//            String jarDirectory = FileUtil.getParent(jarFile.getAbsolutePath(), 5);
//
           String jarDirectory = jarFile.getParent();

           System.setProperty("jar.base",jarDirectory);

           File log4j_file = new File(jarDirectory + "/conf/log4j.properties");

           PropertyConfigurator.configure(log4j_file.getAbsolutePath());

           String setting_path = new File(jarDirectory + "/conf/sync_data.setting").getAbsolutePath();

           setting = new Setting(setting_path);

//           System.out.println("Jar包所在目录：" + jarDirectory);

           System.out.println("" +
                   "  _________________________ _____________________   _____________ ____________ _________ ___________ _________ ________________________ ___.____     \n" +
                   " /   _____/\\__    ___/  _  \\\\______   \\__    ___/  /   _____/    |   \\_   ___ \\\\_   ___ \\\\_   _____//   _____//   _____/\\_   _____/    |   \\    |    \n" +
                   " \\_____  \\   |    | /  /_\\  \\|       _/ |    |     \\_____  \\|    |   /    \\  \\//    \\  \\/ |    __)_ \\_____  \\ \\_____  \\  |    __) |    |   /    |    \n" +
                   " /        \\  |    |/    |    \\    |   \\ |    |     /        \\    |  /\\     \\___\\     \\____|        \\/        \\/        \\ |     \\  |    |  /|    |___ \n" +
                   "/_______  /  |____|\\____|__  /____|_  / |____|    /_______  /______/  \\______  /\\______  /_______  /_______  /_______  / \\___  /  |______/ |_______ \\\n" +
                   "        \\/                 \\/       \\/                    \\/                 \\/        \\/        \\/        \\/        \\/      \\/                    \\/" +
                   "");
       } catch (Exception e) {
           e.printStackTrace();
       }






   }



    public static void main(String[] args) {

        Timer timer = new Timer();

        offline(timer,setting);

        // 程序终止时取消定时任务
        Runtime.getRuntime().addShutdownHook(new Thread(() -> timer.cancel()));
    }

    private static void offline(Timer timer,Setting setting) {

        Map<String,String> folderTimings = setting.getMap("offline");

        // 根据文件夹及定时拉取时间设置定时任务
        for (Map.Entry<String, String> entry : folderTimings.entrySet()) {
            String source_dir = entry.getKey();
            String timing = entry.getValue();

            String[] timeParts = timing.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            int second = Integer.parseInt(timeParts[2]);

            Date firstTaskTime = getNextTime(hour, minute, second);
            long delay = firstTaskTime.getTime() - System.currentTimeMillis();
            if (delay < 0) {
                delay += 24 * 60 * 60 * 1000; // 如果任务时间已过，则从明天开始执行
            }

            if (source_dir.contains("wininfo")){

                special_offline(timer, setting, source_dir, delay);

            }else {
                common_offline(timer, setting, source_dir, delay);
            }


        }
    }

    private static void common_offline(Timer timer, Setting setting, String source_dir, long delay) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {


                SFTPUtil sftp = load_Setting(setting);
                sftp.login();

                String target_dir = setting.get("target_dir", "target_dir");
                String target_date = LocalDate.now().toString();

                try {
                    ArrayList<File> files = sftp.listFiles(source_dir, target_date);
                    if (files.size() > 0) {
                        for (File file : files) {
                            try {
                                sftp.download(file, target_dir);
                            } catch (IOException e) {
                                log.error("download error");
                                e.printStackTrace();
                            }

                        }
                    } else {
//                        log.error(source_dir + "下没有当前日期文件 ：" + target_date);
                        log.error("there are no files of the current date : " + target_date+ " in the source directory  :  " + source_dir);
                    }
                    sftp.logout();
                } catch (SftpException e) {
                    e.printStackTrace();
                }

            }
        }, delay, 24 * 60 * 60 * 1000); // 每隔一天执行一次
    }

    private static void special_offline(Timer timer, Setting setting, String source_dir, long delay) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                String target_date = LocalDate.now().toString();

                List<String> collect = null;

                Db db = Db.use(new SimpleDataSource(
                        setting.get("mysql", "url"),
                        setting.get("mysql", "username"),
                        setting.get("mysql", "password"),
                        setting.get("mysql", "driver")));

                try {
                    collect = db.query("select concat(cwl_id ,issue) as cwl_name  from reckon_issue_info  where CONVERT(end , date) = ? ", target_date)
                            .stream().map(entity -> entity.toBean(JSONObject.class).getString("cwl_name")).collect(Collectors.toList());
                    log.info("当日中奖信息：" + Arrays.toString(collect.toArray()));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

                if(collect != null || collect.size() == 0) {
                    while (collect.size() != 0) {

                        SFTPUtil sftp = load_Setting(setting);
                        sftp.login();

                        String target_dir = setting.get("target_dir", "target_dir");


                        try {
                            ArrayList<File> files = sftp.listFiles(source_dir, target_date);
                            if (files.size() > 0) {

                                for (File file : files) {
                                    String cwl_name = getCwlIdForSerial(file.getName().split("_")[1])+file.getName().split("_")[2];

                                    if (collect.contains(cwl_name)) {
                                        try {

                                            sftp.download(file, target_dir);
                                            collect.remove(cwl_name);

                                        } catch (IOException e) {
                                            log.error("download error");
                                            e.printStackTrace();
                                        }
                                    }else {
                                        log.debug("there are not current files "+ file.getName() + " of the current date : " + target_date + " in the source directory  :  " + source_dir);
                                    }

                                }
                            } else {
//                        log.error(source_dir + "下没有当前日期文件 ：" + target_date);
                                log.debug("there are no files of the current date : " + target_date + " in the source directory  :  " + source_dir);
                            }
                        } catch (SftpException e) {
                            e.printStackTrace();
                        }
                        sftp.logout();

                        try {
                            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    log.error("there are no game on the current date : " + target_date  + " in the source directory  :  " + source_dir);
                }

            }
        }, delay, 24 * 60 * 60 * 1000); // 每隔一天执行一次
    }


    private static Date getNextTime(int hour, int minute, int second) {
        Calendar nextTime = Calendar.getInstance();
        nextTime.set(Calendar.HOUR_OF_DAY, hour);
        nextTime.set(Calendar.MINUTE, minute);
        nextTime.set(Calendar.SECOND, second);

        Calendar now = Calendar.getInstance();
        if (now.after(nextTime)) {
            nextTime.add(Calendar.DATE, 1); // 如果时间已过，则从明天开始执行
        }

        return nextTime.getTime();
    }

    private static String getCwlIdForSerial( String serial) {
        switch (serial) {
            case "B001":
                return "10001";
            case "S3":
                return "10002";
            case "QL730":
                return "10003";
            case "ZCKL8":
                return "10005";
            case "QL515":
                return "90015";
            case "SP61":
                return "90016";
            default:
                return null;
        }
    }
}