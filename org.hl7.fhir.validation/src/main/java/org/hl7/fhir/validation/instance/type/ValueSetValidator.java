package org.hl7.fhir.validation.instance.type;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.terminologies.utilities.CodingValidationRequest;
import org.hl7.fhir.r5.terminologies.utilities.TerminologyServiceErrorClass;
import org.hl7.fhir.r5.terminologies.utilities.ValidationResult;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.validation.BaseValidator;
import org.hl7.fhir.validation.codesystem.CodeSystemChecker;
import org.hl7.fhir.validation.codesystem.GeneralCodeSystemChecker;
import org.hl7.fhir.validation.codesystem.SnomedCTChecker;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.utils.NodeStack;

public class ValueSetValidator extends BaseValidator {

  private static final int TOO_MANY_CODES_TO_VALIDATE = 1000;
  
  private CodeSystemChecker getSystemValidator(String system, List<ValidationMessage> errors) {
    if (system == null) {
      return new GeneralCodeSystemChecker(context, xverManager, debug, errors);
    }
    switch (system) {
    case "http://snomed.info/sct" :return new SnomedCTChecker(context, xverManager, debug, errors);
    default: return new GeneralCodeSystemChecker(context, xverManager, debug, errors);
    }
  }
  
  public class VSCodingValidationRequest extends CodingValidationRequest {

    private NodeStack stack;

    public VSCodingValidationRequest(NodeStack stack, Coding code) {
      super(code);
      this.stack = stack;
    }

    public NodeStack getStack() {
      return stack;
    }
    
  }

  public ValueSetValidator(InstanceValidator parent) {
    super(parent);
  }
  
  public boolean validateValueSet(List<ValidationMessage> errors, Element vs, NodeStack stack) {
    boolean ok = true;
    if (!VersionUtilities.isR2Ver(context.getVersion())) {
      List<Element> composes = vs.getChildrenByName("compose");
      int cc = 0;
      for (Element compose : composes) {
        ok = validateValueSetCompose(errors, compose, stack.push(compose, composes.size() > 1 ? cc : -1, null, null), vs.getNamedChildValue("url", false), "retired".equals(vs.getNamedChildValue("url", false))) & ok;
        cc++;
      }
    }
    if (!stack.isContained()) {
      ok = checkShareableValueSet(errors, vs, stack) && ok;
    }
    return ok;
  }

  private boolean checkShareableValueSet(List<ValidationMessage> errors, Element vs, NodeStack stack) {
    if (parent.isForPublication()) { 
      if (isHL7(vs)) {
        boolean ok = true;
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("url", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "url") && ok;                      
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("version", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "version") && ok;                      
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("title", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "title") && ok;                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("name", false), I18nConstants.VALUESET_SHAREABLE_EXTRA_MISSING_HL7, "name");                      
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("status", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "status") && ok;                      
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("experimental", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "experimental") && ok;                      
        ok = rule(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("description", false), I18nConstants.VALUESET_SHAREABLE_MISSING_HL7, "description") && ok;
        return ok;
      } else {
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("url", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "url");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("version", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "version");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("title", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "title");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("name", false), I18nConstants.VALUESET_SHAREABLE_EXTRA_MISSING, "name");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("status", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "status");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("experimental", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "experimental");                      
        warning(errors, NO_RULE_DATE, IssueType.REQUIRED, vs.line(), vs.col(), stack.getLiteralPath(), vs.hasChild("description", false), I18nConstants.VALUESET_SHAREABLE_MISSING, "description");
      }
    }
    return true;
  }


  private boolean validateValueSetCompose(List<ValidationMessage> errors, Element compose, NodeStack stack, String vsid, boolean retired) {
    boolean ok = true;
    List<Element> includes = compose.getChildrenByName("include");
    int ci = 0;
    for (Element include : includes) {
      ok = validateValueSetInclude(errors, include, stack.push(include, ci, null, null), vsid, retired) && ok;
      ci++;
    }    
    List<Element> excludes = compose.getChildrenByName("exclude");
    int ce = 0;
    for (Element exclude : excludes) {
      ok = validateValueSetInclude(errors, exclude, stack.push(exclude, ce, null, null), vsid, retired) && ok;
      ce++;
    }    
    return ok;
  }
  
  private boolean validateValueSetInclude(List<ValidationMessage> errors, Element include, NodeStack stack, String vsid, boolean retired) {
    boolean ok = true;
    String system = include.getChildValue("system");
    String version = include.getChildValue("version");
    List<Element> valuesets = include.getChildrenByName("valueSet");
    int i = 0;
    for (Element ve : valuesets) {
      String v = ve.getValue();
      ValueSet vs = context.fetchResource(ValueSet.class, v);
      if (vs == null) {
        NodeStack ns = stack.push(ve, i, ve.getProperty().getDefinition(), ve.getProperty().getDefinition());

        Resource rs = context.fetchResource(Resource.class, v);
        if (rs != null) {
          warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, ns.getLiteralPath(), false, I18nConstants.VALUESET_REFERENCE_INVALID_TYPE, v, rs.fhirType());                      
        } else { 
          // todo: it's possible, at this point, that the txx server knows the value set, but it's not in scope
          // should we handle this case?
          warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, ns.getLiteralPath(), false, I18nConstants.VALUESET_REFERENCE_UNKNOWN, v);            
        }
      }
      i++;
    }
    if (valuesets.size() > 1) {
      warning(errors, NO_RULE_DATE, IssueType.INFORMATIONAL, stack.getLiteralPath(), false, I18nConstants.VALUESET_IMPORT_UNION_INTERSECTION);                  
    }
    List<Element> concepts = include.getChildrenByName("concept");
    List<Element> filters = include.getChildrenByName("filter");

    CodeSystemChecker slv = getSystemValidator(system, errors);
    if (!Utilities.noString(system)) {
      boolean systemOk = true;
      int cc = 0;
      List<VSCodingValidationRequest> batch = new ArrayList<>();
      boolean first = true;
      for (Element concept : concepts) {
        // we treat the first differently because we want to know if the system is worth validating. if it is, then we batch the rest
        if (first) {
          systemOk = validateValueSetIncludeConcept(errors, concept, stack, stack.push(concept, cc, null, null), system, version, slv);
          first = false;
        } else if (systemOk) {
          batch.add(prepareValidateValueSetIncludeConcept(errors, concept, stack.push(concept, cc, null, null), system, version, slv));
        }
        cc++;
      }    
      if (((InstanceValidator) parent).isValidateValueSetCodesOnTxServer() && batch.size() > 0 & !context.isNoTerminologyServer()) {
        if (batch.size() > TOO_MANY_CODES_TO_VALIDATE) {
          ok = hint(errors, "2023-09-06", IssueType.BUSINESSRULE, stack.getLiteralPath(), false, I18nConstants.VALUESET_INC_TOO_MANY_CODES, batch.size()) && ok;
        } else {
          long t = System.currentTimeMillis();
          if (parent.isDebug()) {
            System.out.println("  : Validate "+batch.size()+" codes from "+system+" for "+vsid);
          }
          try {
            context.validateCodeBatch(ValidationOptions.defaults(), batch, null);
            if (parent.isDebug()) {
              System.out.println("  :   .. "+(System.currentTimeMillis()-t)+"ms");
            }
            for (VSCodingValidationRequest cv : batch) {
              if (version == null) {
                warningOrHint(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, cv.getStack().getLiteralPath(), cv.getResult().isOk(), !retired, I18nConstants.VALUESET_INCLUDE_INVALID_CONCEPT_CODE, system, cv.getCoding().getCode());
              } else {
                warningOrHint(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, cv.getStack().getLiteralPath(), cv.getResult().isOk(), !retired, I18nConstants.VALUESET_INCLUDE_INVALID_CONCEPT_CODE_VER, system, version, cv.getCoding().getCode());
              }
            }
          } catch (Exception e) {
            ok = false;
            VSCodingValidationRequest cv = batch.get(0);
            rule(errors, NO_RULE_DATE, IssueType.EXCEPTION, cv.getStack().getLiteralPath(), false, e.getMessage());
          }
        }
      }

      int cf = 0;
      for (Element filter : filters) {
        if (systemOk && !validateValueSetIncludeFilter(errors, include, stack.push(filter, cf, null, null), system, version, slv)) {
          systemOk = false;          
        }
        cf++;
      }    
      slv.finish(include, stack);
    } else {
      warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stack.getLiteralPath(), filters.size() == 0 && concepts.size() == 0, I18nConstants.VALUESET_NO_SYSTEM_WARNING);      
    }
    return ok;
  }


  private boolean validateValueSetIncludeConcept(List<ValidationMessage> errors, Element concept, NodeStack stackInc, NodeStack stack, String system, String version, CodeSystemChecker slv) {
    String code = concept.getChildValue("code");
    String display = concept.getChildValue("display");
    slv.checkConcept(code, display);
    
    if (version == null) {
      ValidationResult vv = context.validateCode(ValidationOptions.defaults(), new Coding(system, code, null), null);
      if (vv.getErrorClass() == TerminologyServiceErrorClass.CODESYSTEM_UNSUPPORTED) {
        if (isExampleUrl(system)) {
          if (isAllowExamples()) {
            hint(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stackInc.getLiteralPath(), false, I18nConstants.VALUESET_EXAMPLE_SYSTEM_HINT, system);
          } else {
            rule(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stackInc.getLiteralPath(), false, I18nConstants.VALUESET_EXAMPLE_SYSTEM_ERROR, system);
          }
        } else {
          warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stackInc.getLiteralPath(), false, I18nConstants.VALUESET_UNC_SYSTEM_WARNING, system, vv.getMessage());
        }
        return false;
      } else {
        boolean ok = vv.isOk();
        warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stack.getLiteralPath(), ok, I18nConstants.VALUESET_INCLUDE_INVALID_CONCEPT_CODE, system, code);
        if (vv.getMessage() != null) {
          hint(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stack.getLiteralPath(), false, vv.getMessage());
        }
      }
    } else {
      ValidationResult vv = context.validateCode(ValidationOptions.defaults(), new Coding(system, code, null).setVersion(version), null);
      if (vv.getErrorClass() == TerminologyServiceErrorClass.CODESYSTEM_UNSUPPORTED) {
        warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stackInc.getLiteralPath(), false, I18nConstants.VALUESET_UNC_SYSTEM_WARNING_VER, system+"#"+version, vv.getMessage());            
        return false;        
      } else {
        boolean ok = vv.isOk();
        warning(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stack.getLiteralPath(), ok, I18nConstants.VALUESET_INCLUDE_INVALID_CONCEPT_CODE_VER, system, version, code);
        if (vv.getMessage() != null) {
          hint(errors, NO_RULE_DATE, IssueType.BUSINESSRULE, stack.getLiteralPath(), false, vv.getMessage());
        }
      }
    }
    return true;
  }

  private VSCodingValidationRequest prepareValidateValueSetIncludeConcept(List<ValidationMessage> errors, Element concept, NodeStack stack, String system, String version, CodeSystemChecker slv) {
    String code = concept.getChildValue("code");
    String display = concept.getChildValue("display");
    slv.checkConcept(code, display);
    
    Coding c = new Coding(system, code, null);
    if (version != null) {
       c.setVersion(version);
    }
    return new VSCodingValidationRequest(stack, c);
  }

  private boolean validateValueSetIncludeFilter(List<ValidationMessage> errors, Element filter, NodeStack push, String system, String version, CodeSystemChecker slv) {
//
//    String display = concept.getChildValue("display");
//    slv.checkConcept(code, display);
    
    return true;
  }
}
