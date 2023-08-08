package fujian;

import cn.hutool.setting.Setting;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static fujian.SFTPUtil.load_Setting;

public class SftpMonitor {

    static final Logger log = LoggerFactory.getLogger(SyncFiles.class);



    public static void main(String[] args) {

        String setting_path = args[0];

        Setting setting = new Setting(setting_path);

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

                sprcial_offline(timer, setting, source_dir, delay);

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

    private static void sprcial_offline(Timer timer, Setting setting, String source_dir, long delay) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                boolean res = false;

                while (!res) {

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
                            res = true;
                        } else {
//                        log.error(source_dir + "下没有当前日期文件 ：" + target_date);
                            log.error("there are no files of the current date : " + target_date + " in the source directory  :  " + source_dir);
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
}