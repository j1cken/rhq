/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.client.commands;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.NoOpRecorder;
import org.rhq.enterprise.client.Recorder;
import org.rhq.enterprise.client.script.CommandLineParseException;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;

public class RecordCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "record";
    }

    public boolean execute(ClientMain client, String[] cmdLine) {
        String[] cmdLineArgs = Arrays.copyOfRange(cmdLine, 1, cmdLine.length);

        RecordArgs args = parseArgs(cmdLineArgs);

        if (args.recordState == RecordState.STOP) {
            stopRecording(client);
        }
        else {
            startRecording(client, args);
        }

        return true;
    }

    private RecordArgs parseArgs(String[] args) {
        String shortOpts = "-:f:abe";
        LongOpt[] longOpts = {
                new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("append", LongOpt.OPTIONAL_ARGUMENT, null, 'a'),
                new LongOpt("start", LongOpt.OPTIONAL_ARGUMENT, null, 'b'),
                new LongOpt("end", LongOpt.OPTIONAL_ARGUMENT, null, 'e')
        };
        Getopt getopt = new Getopt(getPromptCommandString(), args, shortOpts, longOpts);
        getopt.setOpterr(false);

        RecordArgs recordArgs = new RecordArgs();

        int code = getopt.getopt();
        while (code != -1) {
            switch (code) {
                case ':':
                case '?':
                   throw new CommandLineParseException("Invalid option");
                case 1:
                    break;
                case 'f':
                    recordArgs.file = new File(getopt.getOptarg());
                    break;
                case 'a':
                    recordArgs.append = true;
                    break;
                case 'b':
                    if (recordArgs.recordState == null) {
                        recordArgs.recordState = RecordState.START;
                    }
                    break;
                case 'e':
                    if (recordArgs.recordState == null) {
                        recordArgs.recordState = RecordState.STOP;
                    }
                    break;
            }
            code = getopt.getopt();
        }

        if (recordArgs.file == null) {
            throw new CommandLineParseException("The file option must be specified.");
        }

        if (recordArgs.recordState == null) {
            throw new CommandLineParseException("Either the start or stop option must be specified.");
        }

        return recordArgs;
    }

    private void stopRecording(ClientMain client) {
        try {
            client.getRecorder().stop();
            client.setRecorder(new NoOpRecorder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecording(ClientMain client, RecordArgs args) {
       try {
           Recorder recorder = new Recorder();
           recorder.setWriter(new FileWriter(args.file, args.append));

           client.setRecorder(recorder);
       } catch (IOException e) {
           e.printStackTrace();
       }
    }

    public String getSyntax() {
        return getPromptCommandString() + " [[-b | --start] | [-e | --end]] [-a | --append] -f <file>";
    }

    public String getHelp() {
        return "Record user input commands to a specified file. Use the --start option to begin recording. Use the " +
            "--end option to stop recording. Use --append to append output to the end of an existing file; otherwise, " +
            "recording will start at the beginning of the file.";
    }

    public String getDetailedHelp() {
        return getHelp();
    }

    private static class RecordArgs {
        boolean append;

        File file;

        RecordState recordState;
    }

    private static enum RecordState { START, STOP }
}
