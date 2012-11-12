package org.mccaughey.pathGenerator;

import java.net.MalformedURLException;
import java.net.URL;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.io.LoadFromRelativeFile;
import org.jbehave.core.junit.JUnitStory;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.reporters.StoryReporterBuilder.Format;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mccaughey.PathGeneratorSteps;

import de.codecentric.jbehave.junit.monitoring.JUnitReportingRunner;

@RunWith(JUnitReportingRunner.class)
public class PathGeneratorTest extends JUnitStory {

  @Override
  public Configuration configuration() {
    URL storyURL = null;

    try {
      // This requires you to start Maven from the project directory
      storyURL = new URL("file://" + System.getProperty("user.dir")
          + "/src/test/resources/stories/");
      System.out.println(storyURL.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return new MostUsefulConfiguration().useStoryLoader(
        new LoadFromRelativeFile(storyURL)).useStoryReporterBuilder(
        new StoryReporterBuilder().withFormats(Format.HTML));
  }

  @Override
  public InjectableStepsFactory stepsFactory() {
    return new InstanceStepsFactory(configuration(), new PathGeneratorSteps());
  }

  @Override
  @Test
  public void run() {
    try {
      // JUnitReportingRunner.recommandedControls(configuredEmbedder());
      super.run();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
