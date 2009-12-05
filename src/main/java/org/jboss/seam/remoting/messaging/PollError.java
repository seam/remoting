package org.jboss.seam.remoting.messaging;

/**
 *
 * @author Shane Bryzak
 */
public class PollError
{
  public static final String ERROR_CODE_TOKEN_NOT_FOUND = "TOKEN_NOT_FOUND";
  public static final String ERROR_CODE_JMS_EXCEPTION = "JMS_EXCEPTION";

  private String code;
  private String message;

  public PollError(String code, String message)
  {
    this.code = code;
    this.message = message;
  }

  public String getCode()
  {
    return code;
  }

  public String getMessage()
  {
    return message;
  }
}
