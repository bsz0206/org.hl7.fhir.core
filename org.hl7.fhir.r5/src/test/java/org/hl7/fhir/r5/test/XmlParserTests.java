package org.hl7.fhir.r5.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.hl7.fhir.r5.context.ContextUtilities;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.ToolsVersion;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager.FilesystemPackageCacheMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class XmlParserTests {

  private static SimpleWorkerContext context;
  private static FHIRPathEngine fp;

  @BeforeAll
  public static void setUp() throws Exception {
    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager(org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager.FilesystemPackageCacheMode.USER);
    context = TestingUtilities.getWorkerContext(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"));

    fp = new FHIRPathEngine(context);

    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "any.xml"), "any.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "ii.xml"), "ii.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "cd.xml"), "cd.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "ce.xml"), "ce.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "ed.xml"), "ed.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "st.xml"), "st.xml", null);
    context.loadFromFile(TestingUtilities.loadTestResourceStream("validator", "cda", "cda.xml"), "cda.xml", null);
    for (StructureDefinition sd : context.fetchResourcesByType(StructureDefinition.class)) {
      if (!sd.hasSnapshot()) {
        System.out.println("generate snapshot for " + sd.getUrl());
        new ContextUtilities(context).generateSnapshot(sd);
      }
    }
  }

  @Test
  /**
   * Deserializes a simplified CDA example into the logical model and checks that
   * xml deserialization works for the xsi:type 
   * 
   * @throws IOException
   */
  public void testXsiDeserialiserXmlParser() throws IOException {
    Element cda = Manager.parseSingle(context, TestingUtilities.loadTestResourceStream("validator", "cda", "example-xsi.xml"),
        FhirFormat.XML);

    ByteArrayOutputStream baosXml = new  ByteArrayOutputStream();
    Manager.compose(context, cda, baosXml, FhirFormat.XML, OutputStyle.PRETTY, null);  

    String cdaSerialised = baosXml.toString();
    Assertions.assertTrue(cdaSerialised.indexOf("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")>0);
    Assertions.assertTrue(cdaSerialised.indexOf("xsi:type=\"CD\"")>0);
  }
} 