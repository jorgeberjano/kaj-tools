package es.jbp.kajtools.util;

import es.jbp.kajtools.ui.JsonGeneratorPanel;
import io.micrometer.core.instrument.util.IOUtils;
import io.micrometer.core.instrument.util.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceUtil {
  public static String readFileString(File file) throws FileNotFoundException {
    InputStream stream = new FileInputStream(file);
    return IOUtils.toString(stream, StandardCharsets.UTF_8);
  }

  public static String readResourceString(String resourceName) {
    if (StringUtils.isBlank(resourceName)) {
      return "";
    }
    return IOUtils.toString(getResourceStream(resourceName), StandardCharsets.UTF_8);
  }

  public static List<String> readResourceStringList(String resourceName) {

    InputStream inputStream = JsonGeneratorPanel.class.getClassLoader()
        .getResourceAsStream(resourceName);
    if (inputStream == null) {
      return Collections.emptyList();
    }
    return new BufferedReader(new InputStreamReader(inputStream,
        StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
  }

  public static InputStream getResourceStream(String resourceName) {
    return ResourceUtil.class.getClassLoader().getResourceAsStream(resourceName);
  }

  public static String getResourcePath(String resourceName) {
    if (resourceName == null) {
      return null;
    }
    URL url = ResourceUtil.class.getClassLoader().getResource(resourceName);
    File file;
    try {
      file = new File(url.toURI());
    } catch (URISyntaxException e) {
      return null;
    }
    return file.getPath();
  }

  public static List<String> getResourceFileNames(String folder)  {
    List<String> list = Collections.emptyList();
    URL url;
    try {
      url = ResourceUtil.class.getClassLoader().getResource(folder);
    } catch (Throwable e) {
      e.printStackTrace();
      return list;
    }

    if (url == null) {
      System.err.println("La carpeta de recursos " + folder + " no existe");
      return list;
    }

    try (Stream<Path> paths = Files.walk(Paths.get(url.toURI()))) {
      list = paths
          .filter(Files::isRegularFile)
          .map(path -> folder + "/" + path.getFileName().toString())
          .collect(Collectors.toList());
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return  list;
  }
}
