package fujian;

import cn.hutool.core.util.StrUtil;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.setting.Setting;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static fujian.SFTPUtil.load_Setting;


public class SyncFiles {

    private static String cur_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    static final Logger log = LoggerFactory.getLogger(SyncFiles.class);


//    static {
//
//        final File log4jConfigFile = new File(System.getProperty("user.dir") + "/log4j.properties");
//        // 手动加载 Log4j 配置文件
//        org.apache.log4j.PropertyConfigurator.configure(log4jConfigFile.getAbsolutePath());
//
//    }





    public static void main(String[] args) throws SftpException {

        String setting_path = args[0];

        log.info(setting_path);

        Setting setting = new Setting(setting_path);

//        String username = setting.get("sftp", "username");
//        String password = setting.get("sftp", "password");
//        String host = setting.get("sftp", "host");
//        Integer port = setting.getInt("port", "sftp");
//        System.out.println("port = " + port);
//        System.out.println("host = " + host);
//        System.out.println("password = " + password);
//        System.out.println("username = " + username);
//        System.out.println("setting = " + setting);
//        System.out.println(setting.get("sync", "source_dir"));
//        System.out.println(setting.get("sync", "target_dir"));
//        System.out.println(setting.get("sync", "target_date"));


        SFTPUtil sftp = load_Setting(setting);

        if(sftp != null) {

            sftp.login();

            String[] source_dirs = StrUtil.splitToArray(setting.get("sync", "source_dir"), ",");

            String target_date = setting.get("sync", "target_date");

            String target_dir = setting.get("sync", "target_dir");

            if (source_dirs.length >0) {


                for (String source_dir : source_dirs) {

                    ArrayList<File> files = null;

                    if (StrUtil.contains(target_date,":")){
                        files = sftp.listFiles(source_dir,target_date.split(":")[0],target_date.split(":")[1]);
                    }else if (StrUtil.contains(target_date,",")){
                        files = sftp.listFiles(source_dir,target_date,true);
                    }else {
                        files = sftp.listFiles(source_dir, target_date);
                    }



                    if (files.size() > 0) {
                        for (File file : files) {
//                System.out.println(file.getAbsolutePath());
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


                }
            }else {
                log.error( " source directory does not exist");
            }

            sftp.logout();

        }
    }
}

