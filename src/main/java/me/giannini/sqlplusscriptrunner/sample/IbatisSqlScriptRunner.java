package me.giannini.sqlplusscriptrunner.sample;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;

import com.ibatis.common.jdbc.ScriptRunner;

import me.giannini.sqlplusscriptrunner.SqlScriptRunner;

public class IbatisSqlScriptRunner implements SqlScriptRunner {

  private final Connection connection;

  public IbatisSqlScriptRunner(final Connection connection) {
    this.connection = connection;
  }

  @Override
  public void execute(final String scriptSource, final int lineOffset) {
    final ScriptRunner runner = new ScriptRunner(connection, false, true);
    try (final LineNumberReader reader = new LineNumberReader(new StringReader(scriptSource))) {
      reader.setLineNumber(lineOffset);
      try {
        runner.runScript(reader);
      } catch (final IOException | SQLException e) {
        throw new ScriptRunException("Script execution failed on line " + reader.getLineNumber(), e);
      }
    } catch (final Exception e) {
      if (e instanceof ScriptRunException) {
        throw (ScriptRunException)e;
      } else {
        throw new ScriptRunException("Script execution failed", e);
      }
    }
  }

  public static class ScriptRunException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ScriptRunException(final String message, final Throwable cause) {
      super(message, cause);
    }

  }

}
