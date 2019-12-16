package com.borland.jenkins.SilkPerformerJenkins.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import hudson.model.BuildListener;

public class SystemUtils
{
  private static boolean isSystemInitialized = false;

  private SystemUtils()
  {
  }

  public static void initSystem(String performerInstallDir, BuildListener listener)
  {
    if (!isSystemInitialized)
    {
      addToJavaLib(performerInstallDir, listener);
      String nativeLibsPath = getNativeLibrariesPath(performerInstallDir);
      if (!performerInstallDir.equalsIgnoreCase(nativeLibsPath))
      {
        addToJavaLib(nativeLibsPath, listener);
      }
      loadNativeLibraries(nativeLibsPath, listener);
      loadSgemJar(performerInstallDir, listener);

      isSystemInitialized = true;
    }
  }

  private static void loadSgemJar(String performerInstallDir, BuildListener listener)
  {
    try
    {
      ClassLoader clsLoader = ClassLoader.getSystemClassLoader();
      if (URLClassLoader.class.isInstance(clsLoader))
      {
        File ff = new File(performerInstallDir + "\\ClassFiles\\sgem.jar");
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), ff.toURI().toURL());

        listener.getLogger().println(ff.getAbsolutePath() + " is loaded!");
      }
      else
      {
        listener.getLogger().println("No URLClassLoader! " + clsLoader.getClass());
      }
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e)
    {
      listener.error("Cannot load sgem.jar.");
      e.printStackTrace(listener.getLogger());
    }
  }

  private static void addToJavaLib(String performerInstallDir, BuildListener listener)
  {
    try
    {
      String s = System.getProperty("java.library.path");
      if (!s.contains(performerInstallDir))
      {
        listener.getLogger().println(performerInstallDir + " is added to java.library.path");
        s = performerInstallDir + ";" + s;
        System.setProperty("java.library.path", s);
        Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
      }
    }
    catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
    {
      listener.error("Cannot extend java library path.");
      e.printStackTrace(listener.getLogger());
    }
  }

  private static void loadNativeLibraries(String performerInstallDir, BuildListener listener)
  {
    try
    {
      System.load(performerInstallDir + "\\locStringsCommon.dll");
      System.load(performerInstallDir + "\\locStringsKernelCommon.dll");
      System.load(performerInstallDir + "\\locStringsKernelFeatures.dll");
      System.load(performerInstallDir + "\\perfVersion.dll");
      System.load(performerInstallDir + "\\perfBexScanner.dll");
      System.load(performerInstallDir + "\\perfXmlReports.dll");
      System.load(performerInstallDir + "\\perfTsd.dll");
      System.load(performerInstallDir + "\\perfBdlScanner.dll");
      System.load(performerInstallDir + "\\sgExecManager.dll");
      System.load(performerInstallDir + "\\perfMessages.dll");
      System.load(performerInstallDir + "\\perfLm.dll");
      // We need to load these libraries to be able to load sgemBridge.dll
    }
    catch (Exception e)
    {
      listener.error("Cannot load SP specific native libraries.");
      e.printStackTrace(listener.getLogger());
    }
  }

  private static String getNativeLibrariesPath(String performerInstallDir)
  {
    if (System.getProperty("sun.arch.data.model").equals("64"))
    {
      Path p = Paths.get(performerInstallDir, "X64");
      if (p.toFile().exists())
      {
        return p.toString();
      }

      String programFiles86 = System.getenv("ProgramFiles(X86)");
      if (performerInstallDir.contains(programFiles86))
      {
        String programFiles = System.getenv("ProgramFiles");
        String s = performerInstallDir.replace(programFiles86, programFiles);
        if (new File(s).exists())
        {
          return s;
        }
      }
    }
    return performerInstallDir;
  }
}
