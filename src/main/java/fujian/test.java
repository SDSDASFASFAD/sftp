package fujian;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.setting.Setting;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class test {

    static {
//        File file = new File(System.getProperty("user.dir") + "/conf/log4j.properties");
//        // 加载log4j配置文件
//        PropertyConfigurator.configure(file.getAbsolutePath());

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
            System.out.println("Jar包所在目录：" + jarDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) {


        String setting_path = new File(System.getProperty("user.dir") + "/conf/sync_data.setting").getAbsolutePath();

        Setting setting = new Setting(setting_path);
//        final Logger log = LoggerFactory.getLogger(test.class);

//        log.info(setting.get("mysql", "url"));
//        log.info(setting.get("mysql", "username"));
//        log.info(setting.get("mysql", "password"));
//        log.info(setting.get("mysql", "driver"));


//        SimpleDataSource simpleDataSource = new SimpleDataSource(
//                setting.get("mysql", "url"),
//                setting.get("mysql", "username"),
//                setting.get("mysql", "password"),
//                setting.get("mysql", "driver"));
//        Db db = Db.use(simpleDataSource);

//        try {
//            List<Entity> query = db.query("select *  from reckon_issue_info ");
//            for (Entity entity : query) {
//                System.out.println(entity.toString());
//            }
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }


    }
}
