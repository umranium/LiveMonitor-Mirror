/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier.sql_data;

import activityclassifier.Activity;
import activityclassifier.Common;
import activityclassifier.utils.ActivityIO;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author oka
 */
public class ParseRawSqlData {

    public static void main(String[] args) {
        try {
            RawSqlDataParser parseDataSql = new RawSqlDataParser(Common.RAW_SQL_DATA);
            System.out.println("Parsing Raw SQL Data from "+Common.RAW_SQL_DATA);
            List<Activity> activities = parseDataSql.scan();

            System.out.println("Saving dirty sql data to "+Common.DIRTY_SQL_DATA);
            PrintStream ps = new PrintStream(Common.DIRTY_SQL_DATA);
            ActivityIO.write(ps, activities);
            ps.close();
            System.out.println("Activities Written: "+activities.size());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
