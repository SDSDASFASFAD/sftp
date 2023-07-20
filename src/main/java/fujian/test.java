package fujian;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;


public class test {



    public static void main(String[] args) {

        final Logger log = LoggerFactory.getLogger(test.class);

        log.info("**************");

        String target_date = "2023-01-01:2023-01-02";

//        System.out.println("*asdasdasdasda");

        if (StrUtil.contains(target_date,":")){
            Date start = DateUtil.parseDate(target_date.split(":")[0]);
            Date end = DateUtil.parseDate(target_date.split(":")[1]);


            while (DateUtil.between(start,end, DateUnit.DAY,false) >= 0 ){

                System.out.println(start);

                start = DateUtil.offsetDay(start,1);

            }
        }else if (StrUtil.contains(target_date,",")){
            System.out.println();
        }else {
            System.out.println();
        }


    }
}
