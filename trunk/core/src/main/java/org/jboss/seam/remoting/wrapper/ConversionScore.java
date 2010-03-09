package org.jboss.seam.remoting.wrapper;

/**
 *
 * @author Shane Bryzak
 */
public enum ConversionScore
{
  nomatch(0),
  compatible(1),
  exact(2);

  private int score;

  ConversionScore(int score)
  {
    this.score = score;
  }

  public int getScore()
  {
    return score;
  }
}
