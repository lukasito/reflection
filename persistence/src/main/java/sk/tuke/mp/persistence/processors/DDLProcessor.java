package sk.tuke.mp.persistence.processors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Collectors;


@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DDLProcessor extends AbstractProcessor {


  private final JpaProcessor entityProcessor;

  public DDLProcessor() {
    super();
    entityProcessor = new EntityProcessor(new FieldProcessor());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    String sql = roundEnv.getElementsAnnotatedWith(Entity.class).stream()
      .filter(element -> element.getKind() == ElementKind.CLASS)
      .map(entityProcessor)
      .collect(Collectors.joining(";\n"));

    writeSqlToFile(sql);
    return true;
  }

  private void writeSqlToFile(String sql) {
    try {
      FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "sql");
      Writer writer = fo.openWriter();
      writer.append(sql);
      writer.close();
    } catch (IOException e) {
      // TODO handle IO
    }
  }
}
