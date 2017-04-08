package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.JpaProcessor;
import sk.tuke.mp.persistence.processors.ProcessingException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static sk.tuke.mp.persistence.processors.JpaProcessor.notEmptyResult;


@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DDLProcessor extends AbstractProcessor {

  private JpaProcessor entityProcessor;
  private JpaProcessor primaryKeyProcessor;
  private JpaProcessor foreignKeyProcessor;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    entityProcessor = new DDLEntityProcessor(new DDLColumnProcessor());
    primaryKeyProcessor = new DDLPrimaryKeyProcessor();
    foreignKeyProcessor = new DDLForeignKeyProcessor(processingEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      Set<? extends Element> entities = roundEnv.getElementsAnnotatedWith(Entity.class);
      String tables = entities.stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(entityProcessor)
        .collect(joining(";\n\n"));

      String primaryKeys = entities.stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .flatMap(elem -> elem.getEnclosedElements().stream())
        .filter(elem -> elem.getKind() == ElementKind.FIELD)
        .map(primaryKeyProcessor)
        .filter(notEmptyResult)
        .collect(joining(";\n"));

      String foreignKeys = entities.stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .flatMap(elem -> elem.getEnclosedElements().stream())
        .filter(elem -> elem.getKind() == ElementKind.FIELD)
        .map(foreignKeyProcessor)
        .filter(notEmptyResult)
        .collect(joining(";\n"));

      String sql = Stream.of(tables, primaryKeys, foreignKeys).collect(joining(";\n\n"));
      writeSqlToFile(sql + ";");
    } catch (ProcessingException e) {
      printProcessingError(e);
      return false;
    } catch (IOException e) {
      printWarning(e);
    }
    return true;
  }

  private void writeSqlToFile(String sql) throws IOException {
    FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "ddl.sql");
    try (Writer writer = fo.openWriter()) {
      writer.append(sql);
    }
  }

  private void printProcessingError(ProcessingException e) {
    processingEnv.getMessager().printMessage(ERROR, e.getMessage(), e.element());
  }

  private void printWarning(Exception e) {
    processingEnv.getMessager().printMessage(WARNING, "Exception occured while generating ddl: " + e.getMessage());
  }
}
