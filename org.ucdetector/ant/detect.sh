#!/bin/bash
# ------------------------------------------------------------------------------
#  Copyright (c) 2009 Joerg Spieler All rights reserved. This program and the
#  accompanying materials are made available under the terms of the Eclipse
#  Public License v1.0 which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------------------

# 'LAUNCHER' must point to launcher.jar which is located in ECLIPSE_HOME/plugins/
export LAUNCHER=/opt/eclipse-3.4/plugins/org.eclipse.equinox.launcher_1.0.100.v20080509-1800.jar
#export LAUNCHER=/opt/eclipse-3.3/plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar
#export LAUNCHER=/opt/eclipse-3.4M5/plugins/org.eclipse.equinox.launcher_1.0.100.v20071211.jar


# WORKSPACE must point to your workspace directory (usually parent directory of java projects)
export WORKSPACE=~/workspace/runtime-EclipseApplication

REM ----------------------------------------------------------------------------
REM FOR MORE OPTIONS, CHECK build.xml!
REM ----------------------------------------------------------------------------
java -jar $LAUNCHER -application org.ucdetector.ucd -consolelog  -data $WORKSPACE -debug


# OR USE eclipse executable:
# /opt/eclipse-3.5M6/eclipse -application org.ucdetector.ucd -nosplash -data $WORKSPACE
