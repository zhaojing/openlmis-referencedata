package org.openlmis.referencedata.web;

import org.openlmis.referencedata.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller used for displaying service's version information.
 */
@RestController
public class VersionController {

  private static final Logger LOGGER = LoggerFactory.getLogger(VersionController.class);

  /**
   * Displays version information.
   *
   * @return {Version} Returns version read from file.
   */
  @RequestMapping("/referencedata")
  public Version display() {
    LOGGER.debug("Returning version");
    return new Version();
  }
}
