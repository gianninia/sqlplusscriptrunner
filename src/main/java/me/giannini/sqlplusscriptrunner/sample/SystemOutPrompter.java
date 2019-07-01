package me.giannini.sqlplusscriptrunner.sample;

import me.giannini.sqlplusscriptrunner.Prompter;

public class SystemOutPrompter implements Prompter {

  @Override
  public void info(final String message) {
    System.out.println(message);
  }

  @Override
  public void debug(final String message) {
    System.out.println(message);
  }

}
