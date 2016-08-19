# Visits & Protocols Plugin for XNAT 1.7 #

This is a fork of the Visits & Protocols module from XNAT 1.6.5 implemented as an XNAT 1.7 plugin.

The latest built versions of this plugin can be downloaded here: http://marketplace.xnat.org/plugin/visits-protocols-plugin-alpha-xnat-1-7/

Here is some documentation in the form of a presentation I've given at the 2016 XNAT Workshop:

https://docs.google.com/presentation/d/1Vkc3hSc76spnuYlXJcaNimP1r-_0MKeWIIrsKpbnGc8/edit?usp=sharing

Formal Wiki documentation on the plugin is in the works following an official release of core XNAT 1.7

# Building #

To build the plugin, run the following command from within the plugin folder:

```bash
./gradlew jar
```

On Windows, you may need to run:

```bash
gradlew jar
```

If you haven't previously run this build, it may take a while for all of the dependencies to download.

You can verify your completed build by looking in the folder **build/libs**. It should contain a file named something like **protocols-plugin-1.0.0-SNAPSHOT.jar**. This is the plugin jar that you can install in your XNAT's **plugins** folder.

## Installing ##

Installing the plugin is as simple as stopping the Tomcat server running your XNAT 1.7 application, copying the plugin jar above into your **plugins** folder, and restarting Tomcat. To verify that the plugin installed correctly:

# Log into your XNAT as an administrator once it's completed the start-up process.
# Click the menu command **Administer->Protocols**.
# And follow this updated tutorial documentation to get started:

For Administrators and Project Owners...
Protocol Management: https://wiki.xnat.org/pages/viewpage.action?pageId=21037137

For Researchers & Data Collection Personnel
Subject Visit Workflow: https://wiki.xnat.org/pages/viewpage.action?pageId=21495813
