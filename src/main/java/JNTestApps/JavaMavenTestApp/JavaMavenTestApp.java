package JNTestApps.JavaMavenTestApp;

import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;

public class JavaMavenTestApp
{
  public static void vulnerableMethod() {
    //FilenameUtils.getPrefixLength("file.txt");
    FilenameUtils.concat("", "");
  }

  public static void nonVulnerableMethod() {
    Lists.newArrayList(1, 2);
  }
  
  public static void main(String[] args)
  {
    if (args.length < 0)
    {
      vulnerableMethod();
    }
    nonVulnerableMethod();
    
    System.out.println("JavaMavenTestApp Exiting Normally.");
  }
}
