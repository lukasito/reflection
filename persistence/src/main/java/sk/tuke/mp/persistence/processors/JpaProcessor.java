package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import java.util.function.Function;

interface JpaProcessor extends Function<Element, String> {

  /**
   * @param element entity or columns
   * @return generated sql
   * @throws ProcessingException
   */
  @Override
  String apply(Element element);
}
