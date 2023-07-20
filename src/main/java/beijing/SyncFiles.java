package beijing;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.Setting;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class SyncFiles {

    public static String target_time;

    public static final Logger log = LoggerFactory.getLogger(SFTPUtil.class);

    public static SFTPUtil load_Setting(Setting setting){

        String username = setting.get("sftp", "username");
        String password = setting.get("sftp", "password");
        String host = setting.get("sftp", "host");
        Integer port = setting.getInt("port", "sftp");
//        System.out.println("post = " + port);
//        System.out.println("host = " + host);
//        System.out.println("password = " + password);
//        System.out.println("username = " + username);
//        System.out.println("setting = " + setting);
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password) || StrUtil.isBlank(host) ){
            log.debug("SFTP 参数缺失");
            return  null;
        }
        return new SFTPUtil(username, password, host, port);

    }


    public static void main(String[] args) throws SftpException, InterruptedException {

        String setting_path = args[0];

        Setting setting = new Setting(setting_path);

        target_time = StrUtil.isBlank(setting.get("sync", "target_time")) ?
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : setting.get("sync", "target_time");


        SFTPUtil sftp = load_Setting(setting);

        if(sftp != null) {

            sftp.login();

            String[] source_dirs = StrUtil.splitToArray(setting.get("sync", "source_dir"), ",");

//            String target_date = setting.get("sync", "target_date");



            String target_dir = setting.get("sync", "target_dir");

            if (source_dirs.length >0) {

                if(!StrUtil.isBlank(setting.get("sync", "target_time"))) {
                    extracted(target_time, sftp, source_dirs, target_dir);
                }else {
                    while (true){

                        extracted(target_time, sftp, source_dirs, target_dir);

                        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                    }
                }


            }else {
                log.error( " source directory does not exist");
            }

            sftp.logout();

        }
    }

    private static void extracted(String target_time, SFTPUtil sftp, String[] source_dirs, String target_dir) throws SftpException {

        for (String source_dir : source_dirs) {

            ArrayList<File> files = sftp.listFiles(source_dir, target_time);

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
                log.error("there are no files of the current date : " + LocalDate.now() +" " + LocalTime.now()  + " in the source directory  :  " + source_dir);
            }


        }
    }
}

