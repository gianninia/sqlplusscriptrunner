package me.giannini.sqlplusscriptrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A rudimentary SQL*Plus runner with limited command support. Currently
 * supported commands are:<br>
 * <ul>
 * <li><b>prompt</b> -&gt; logs content in {@code INFO} level</li>
 * <li><b>@</b> -&gt; includes a file to be added to the runner</li>
 * </ul>
 * All other commands are ignored, i. e. lines that start with the
 * {@link #IGNORED_COMMANDS} are ignored during execution. Supported commands
 * are expected to be in <b>one single line</b>. All other lines are simply
 * passed to the SQL executor.
 */
public class SqlPlusScriptRunner {

  /**
   * SQL*Plus commands that are ignored and not passed to the SQL executor
   */
  public static final Set<String> IGNORED_COMMANDS = new HashSet<>(Arrays.asList(
      "set", "def", "var", "accept", "connect", "spool", "pause", "exit", "sqlplus", "help",
      "host", "show", "startup", "describe", "edit", "get", "save", "list", "del", "input", "append",
      "change", "run", "execute", "disconnect", "shutdown", "--"));

  private final SqlScriptRunner scriptRunner;
  private final Prompter prompter;

  public SqlPlusScriptRunner(final SqlScriptRunner scriptRunner, final Prompter prompter) {
    this.scriptRunner = scriptRunner;
    this.prompter = prompter;
  }

  /**
   * Runs the passed script.
   *
   * @param scriptSource
   *          - the script to run
   * @throws IOException
   *           - if something goes wrong during parsing
   */
  public void runScript(final String scriptSource) throws IOException {
    runScript(parse(new StringReader(scriptSource), new File(".")));
  }

  /**
   * Runs the script read from the passed file using the default
   * {@link Charset}.
   *
   * @param file
   *          - the file containing the script to run
   * @throws IOException
   *           - if something goes wrong during parsing
   */
  public void runScript(final File file) throws IOException {
    runScript(file, Charset.defaultCharset());
  }

  /**
   * Runs the script read from the passed file using the passed {@link Charset}.
   *
   * @param file
   *          - the file containing the script to run
   * @param charset
   *          - the {@link Charset} to use when reading the file
   * @throws IOException
   *           - if something goes wrong during parsing
   */
  public void runScript(final File file, final Charset charset) throws IOException {
    runScript(parse(new InputStreamReader(new FileInputStream(file), charset), file.getParentFile()));
  }

  private void runScript(final List<ScriptFragment> script) throws IOException {
    for (final ScriptFragment fragment : script) {
      fragment.run();
    }
  }

  private List<ScriptFragment> parse(final Reader sourceReader, final File currentFolder) throws IOException {
    final List<ScriptFragment> script = new ArrayList<>();
    ScriptFragment currentSqlFragment = null;
    int currentLine = 0;
    try (BufferedReader reader = new BufferedReader(sourceReader)) {
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        currentLine++;
        final FragmentType fragmentType = FragmentType.fromLine(line);
        if (fragmentType != FragmentType.SQL) {
          script.add(new ScriptFragment(line, fragmentType, this, currentFolder, currentLine));
          currentSqlFragment = null;
        } else {
          if (currentSqlFragment == null) {
            currentSqlFragment = new ScriptFragment(line, fragmentType, this, currentFolder, currentLine);
            script.add(currentSqlFragment);
          } else {
            currentSqlFragment.addLine(line);
          }
        }
      }
    }
    return script;
  }

  private enum FragmentType {
    PROMPT {

      @Override
      void run(final ScriptFragment fragment) throws IOException {
        fragment.getPrompter().info(fragment.getLines().trim().substring("prompt".length()).trim());
      }

      @Override
      boolean lineMatches(final String line) {
        return doesFirstWordMatches(line, false, word -> word.startsWith("prompt"));
      }
    },
    INCLUDE {

      @Override
      void run(final ScriptFragment fragment) throws IOException {
        final File includeFile = new File(fragment.getCurrentFolder(), fragment.getLines().trim().substring("@".length()));
        if (includeFile.exists()) {
          fragment.runner.runScript(includeFile);
        } else {
          throw new IllegalArgumentException("Could not find file " + includeFile.getAbsolutePath());
        }
      }

      @Override
      boolean lineMatches(final String line) {
        return doesFirstWordMatches(line, false, word -> word.startsWith("@"));
      }
    },
    IGNORE {

      @Override
      void run(final ScriptFragment fragment) throws IOException {
        // nop
      }

      @Override
      boolean lineMatches(final String line) {
        return doesFirstWordMatches(line, true, word -> IGNORED_COMMANDS.contains(word.toLowerCase()));
      }
    },
    SQL {

      @Override
      void run(final ScriptFragment fragment) throws IOException {
        final String scriptSource = fragment.getLines();
        System.out.println("Executing script:\n" + scriptSource);
        fragment.getPrompter().debug("Executing script:\n" + scriptSource);
        fragment.getScriptRunner().execute(scriptSource, fragment.getLineOffset());
      }

      @Override
      boolean lineMatches(final String line) {
        return true;
      }
    };

    abstract void run(ScriptFragment fragment) throws IOException;

    abstract boolean lineMatches(String line);

    private static boolean doesFirstWordMatches(final String line, final boolean matchEmptyLines, final Function<String, Boolean> firstWordMatcher) {
      return Stream.of(line.split("\\s")).filter(word -> {
        return word.length() > 0;
      }).findFirst().map(firstWordMatcher).orElse(matchEmptyLines);
    }

    public static FragmentType fromLine(final String line) {
      return Stream.of(FragmentType.values()).filter(type -> type.lineMatches(line)).findFirst().orElse(SQL);
    }
  }

  private static class ScriptFragment {

    private final StringBuilder lines;
    private final FragmentType type;
    private final SqlPlusScriptRunner runner;
    private final File currentFolder;
    private final int lineOffset;

    private ScriptFragment(final String firstLine, final FragmentType type, final SqlPlusScriptRunner runner, final File currentFolder, final int lineOffset) {
      this.lines = new StringBuilder().append(firstLine);
      this.type = type;
      this.runner = runner;
      this.currentFolder = currentFolder;
      this.lineOffset = lineOffset;
    }

    public void addLine(final String line) {
      lines.append("\n").append(line);
    }

    public Prompter getPrompter() {
      return runner.prompter;
    }

    public SqlScriptRunner getScriptRunner() {
      return runner.scriptRunner;
    }

    public File getCurrentFolder() {
      return currentFolder;
    }

    public String getLines() {
      return lines.toString();
    }

    public int getLineOffset() {
      return lineOffset;
    }

    public void run() throws IOException {
      type.run(this);
    }

  }
}
