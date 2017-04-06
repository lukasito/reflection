package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;

public class ProcessingException extends RuntimeException {

  private final Element element;

  public ProcessingException(String message, Element element) {
    super(message);
    this.element = element;
  }

  public Element element() {
    return element;
  }
}
