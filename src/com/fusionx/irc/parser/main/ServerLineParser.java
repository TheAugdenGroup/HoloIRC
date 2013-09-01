/*
    HoloIRC - an IRC client for Android

    Copyright 2013 Lalit Maganti

    This file is part of HoloIRC.

    HoloIRC is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HoloIRC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HoloIRC. If not, see <http://www.gnu.org/licenses/>.
 */

package com.fusionx.irc.parser.main;

import android.content.Context;
import android.util.Log;

import com.fusionx.common.Utils;
import com.fusionx.irc.constants.ServerCommands;
import com.fusionx.irc.core.Server;
import com.fusionx.irc.listeners.CoreListener;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import static com.fusionx.common.Constants.LOG_TAG;

public class ServerLineParser {
    @Getter(AccessLevel.PACKAGE)
    private final Server server;

    @Getter(AccessLevel.PACKAGE)
    @Setter
    private boolean disconnectSent;

    private final ServerCodeParser codeParser;
    private final ServerCommandParser commandParser;

    public ServerLineParser(final Context context, final Server server) {
        this.server = server;
        commandParser = new ServerCommandParser(context, this);
        codeParser = new ServerCodeParser(context, this);
    }

    /**
     * A loop which reads each line from the server as it is received and passes it on
     * to parse
     *
     * @param reader - the reader associated with the server stream
     */
    public void parseMain(final BufferedReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final boolean quit = parseLine(line);
                if (quit) {
                    return;
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Parses a line from the server
     *
     * @param line - the raw line from the server
     * @return - returns a boolean which indicates whether the server has disconnected
     */
    boolean parseLine(final String line) {
        final ArrayList<String> parsedArray = Utils.splitRawLine(line, true);
        switch (parsedArray.get(0)) {
            case ServerCommands.Ping:
                // Immediately return
                final String source = parsedArray.get(1);
                CoreListener.respondToPing(server.getWriter(), source);
                return false;
            case ServerCommands.Error:
                // We are finished - the server has kicked us out for some reason
                return true;
            default:
                // Check if the second thing is a code or a command
                if (StringUtils.isNumeric(parsedArray.get(1))) {
                    codeParser.parseCode(parsedArray);
                    return false;
                } else {
                    return commandParser.parseCommand(parsedArray, line, disconnectSent);
                }
        }
    }
}