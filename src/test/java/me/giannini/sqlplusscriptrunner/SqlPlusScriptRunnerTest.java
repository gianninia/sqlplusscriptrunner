package me.giannini.sqlplusscriptrunner;

import static org.mockito.Mockito.inOrder;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SqlPlusScriptRunnerTest {

  @Mock
  private SqlScriptRunner scriptRunner;
  @Mock
  private Prompter prompter;
  @InjectMocks
  private SqlPlusScriptRunner testee;

  @Test
  public void runScript_RunMainScript_CorrectParsing() throws Exception {
    // arrange
    final File scriptFile = new File("src/test/resources/sqlplus_samples/main_script.sql");
    // act
    testee.runScript(scriptFile);
    // assert
    final InOrder inOrder = inOrder(scriptRunner, prompter);
    verifyPromptMessage(inOrder, "starting main script now");
    verifyPromptMessage(inOrder, "running pure sql file");
    verifyScriptExecuted(inOrder, "INSERT INTO A_TABLE (ID, A_FIELD) VALUES (1, 'A_VALUE');", 2);
    verifyScriptExecuted(inOrder, "UPDATE A_TABLE SET A_FIELD = 'A_VALUE';", 5);
    verifyScriptExecuted(inOrder, "" +
        "DECLARE\n" +
        "  my_var INT;\n" +
        "BEGIN\n" +
        "  my_var := my_function('TEST');\n" +
        "  COMMIT;\n" +
        "END;\n" +
        "/", 8);
    verifyPromptMessage(inOrder, "running some other sql");
    verifyScriptExecuted(inOrder, "INSERT INTO A_TABLE (ID, A_FIELD) VALUES (1, 'A_VALUE');", 8);
    verifyPromptMessage(inOrder, "running include sql file");
    verifyPromptMessage(inOrder, "this is a test prompt");
    verifyPromptMessage(inOrder, "running sql now");
    verifyScriptExecuted(inOrder, "UPDATE A_TABLE SET A_FIELD = 'A_VALUE';", 5);
    verifyScriptExecuted(inOrder, "ALTER TABLE A_TABLE MODIFY(ADD ANOTHER_FIELD INTEGER);", 6);
  }

  private void verifyPromptMessage(final InOrder inOrder, final String message) {
    inOrder.verify(prompter).info(message);
  }

  private void verifyScriptExecuted(final InOrder inOrder, final String scriptSource, final int lineOffset) {
    inOrder.verify(prompter).debug("Executing script:\n" + scriptSource);
    inOrder.verify(scriptRunner).execute(scriptSource, lineOffset);
  }

}
