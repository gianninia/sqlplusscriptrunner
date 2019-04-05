package me.giannini.sqlplusscriptrunner;

public interface SqlScriptRunner {

  void execute(final String scriptSource, final int lineOffset);

}
