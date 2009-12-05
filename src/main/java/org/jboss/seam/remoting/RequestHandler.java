package org.jboss.seam.remoting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Shane Bryzak
 */
public interface RequestHandler
{
  static final byte[] ENVELOPE_TAG_OPEN = "<envelope>".getBytes();
  static final byte[] ENVELOPE_TAG_CLOSE = "</envelope>".getBytes();
  static final byte[] BODY_TAG_OPEN = "<body>".getBytes();
  static final byte[] BODY_TAG_CLOSE = "</body>".getBytes();
  static final byte[] REFS_TAG_OPEN = "<refs>".getBytes();
  static final byte[] REFS_TAG_CLOSE = "</refs>".getBytes();
  static final byte[] REF_TAG_OPEN_START = "<ref id=\"".getBytes();
  static final byte[] REF_TAG_OPEN_END = "\">".getBytes();
  static final byte[] REF_TAG_CLOSE = "</ref>".getBytes();

  void handle(HttpServletRequest request, HttpServletResponse response)
      throws Exception;
}
