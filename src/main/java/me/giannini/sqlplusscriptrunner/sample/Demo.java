/**
 *
 */
package me.giannini.sqlplusscriptrunner.sample;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import me.giannini.sqlplusscriptrunner.SqlPlusScriptRunner;

public class Demo {

  public static void main(final String[] args) {
    try {
      final Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "USER", "PASSWORD");
      final SqlPlusScriptRunner runner = new SqlPlusScriptRunner(new IbatisSqlScriptRunner(connection), new SystemOutPrompter());
      runner.runScript(new File("src/test/resources/sqlplus_samples/main_script.sql"));
    } catch (final SQLException | IOException e) {
      e.printStackTrace();
    }
  }
}
