package org.hl7.fhir.validation.cli.tasks;

import java.io.IOException;
import java.io.PrintStream;

import org.hl7.fhir.utilities.SystemExitManager;
import org.hl7.fhir.utilities.TimeTracker;
import org.hl7.fhir.utilities.json.JsonException;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;
import org.hl7.fhir.validation.cli.model.CliContext;
import org.hl7.fhir.validation.cli.utils.Params;
import org.hl7.fhir.validation.special.TxTester;

public class TxTestsTask extends StandaloneTask{
  @Override
  public String getName() {
    return "txTests";
  }

  @Override
  public String getDisplayName() {
    return "Terminology Tests";
  }

  @Override
  public boolean isHidden() {
    return true;
  }

  @Override
  public boolean shouldExecuteTask(CliContext cliContext, String[] args) {
    return Params.hasParam(args, Params.TX_TESTS);
  }

  @Override
  public void printHelp(PrintStream out) {

  }

  @Override
  public void executeTask(CliContext cliContext, String[] args, TimeTracker tt, TimeTracker.Session tts) throws Exception {
      final String source = Params.getParam(args, Params.SOURCE);
      final String output = Params.getParam(args, Params.OUTPUT);
      final String version = Params.getParam(args, Params.VERSION);
      final String tx = Params.getParam(args, Params.TERMINOLOGY);
      final String filter = Params.getParam(args, Params.FILTER);
      final String externals = Params.getParam(args, Params.EXTERNALS);
      boolean ok = new TxTester(new TxTester.InternalTxLoader(source, output), tx, false, loadExternals(externals)).setOutput(output).execute(version, cliContext.getModeParams(), filter);
      SystemExitManager.setError(ok ? 1 : 0);
      SystemExitManager.finish();
  }

  private JsonObject loadExternals(String externals) throws JsonException, IOException {
    if (externals == null) {
      return null;
    } else {
      return JsonParser.parseObjectFromFile(externals);
    }
  }
}
